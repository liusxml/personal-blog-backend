package com.blog.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.base.BaseServiceImpl;
import com.blog.common.exception.BusinessException;
import com.blog.common.exception.SystemErrorCode;
import com.blog.common.security.JwtTokenProvider;
import com.blog.system.api.dto.LoginDTO;
import com.blog.system.api.dto.RegisterDTO;
import com.blog.system.api.dto.UserDTO;
import com.blog.system.api.vo.LoginVO;
import com.blog.system.api.vo.UserVO;
import com.blog.system.constant.RoleConstants;
import com.blog.system.converter.UserConverter;
import com.blog.system.domain.entity.RoleEntity;
import com.blog.system.domain.entity.UserEntity;
import com.blog.system.mapper.RoleMapper;
import com.blog.system.mapper.UserMapper;
import com.blog.system.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@Service
public class UserServiceImpl extends BaseServiceImpl<UserMapper, UserEntity, UserVO, UserDTO, UserConverter>
        implements IUserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    @Value("${app.security.jwt-expiration:7200000}")
    private Long jwtExpiration;

    public UserServiceImpl(UserConverter userConverter, UserMapper userMapper, RoleMapper roleMapper,
                           PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        super(userConverter);
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterDTO registerDTO) {
        log.info("用户注册: username={}, email={}", registerDTO.getUsername(), registerDTO.getEmail());

        // 检查用户名是否已存在
        UserEntity existingUser = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUsername, registerDTO.getUsername()));
        if (existingUser != null) {
            throw new BusinessException(SystemErrorCode.USERNAME_ALREADY_EXISTS);
        }

        // 检查邮箱是否已存在
        existingUser = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getEmail, registerDTO.getEmail()));
        if (existingUser != null) {
            throw new BusinessException(SystemErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 创建用户实体
        UserEntity user = new UserEntity();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setNickname(registerDTO.getNickname());
        user.setStatus(1); // 默认正常状态
        user.setVersion(0);

        userMapper.insert(user);

        // 分配默认角色 (USER)
        RoleEntity userRole = roleMapper.selectOne(
                new LambdaQueryWrapper<RoleEntity>()
                        .eq(RoleEntity::getRoleKey, RoleConstants.DEFAULT_USER_ROLE));
        if (userRole != null) {
            roleMapper.assignRoleToUser(user.getId(), userRole.getId());
        }

        log.info("用户注册成功: userId={}", user.getId());

        // 转换为 VO 返回
        UserVO userVO = converter.entityToVo(user);
        userVO.setRoles(List.of("ROLE_USER"));
        return userVO;
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        log.info("用户登录: username={}", loginDTO.getUsername());

        // 查询用户
        UserEntity user = userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUsername, loginDTO.getUsername())
                        .or()
                        .eq(UserEntity::getEmail, loginDTO.getUsername()) // 支持邮箱登录
        );

        if (user == null) {
            throw new BusinessException(SystemErrorCode.INVALID_CREDENTIALS);
        }

        // 验证密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(SystemErrorCode.INVALID_CREDENTIALS);
        }

        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException(SystemErrorCode.USER_DISABLED);
        }

        // 查询用户角色（带缓存）
        List<String> roleKeys = getUserRoleKeys(user.getId());

        // 构造 UserDetails 用于生成 Token
        List<GrantedAuthority> authorities = roleKeys.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UserDetails userDetails = User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .build();

        // 生成 JWT Token
        String token = jwtTokenProvider.generateToken(userDetails, user.getId());

        // 构造返回值
        UserVO userVO = converter.entityToVo(user);
        userVO.setRoles(roleKeys);

        LoginVO loginVO = LoginVO.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000) // 转换为秒
                .user(userVO)
                .build();

        log.info("用户登录成功: userId={}", user.getId());
        return loginVO;
    }

    /**
     * 更新用户信息
     * <p>
     * 更新后自动清除用户角色缓存，确保数据一致性
     *
     * @param dto 用户更新 DTO
     * @return 更新结果
     */
    @Override
    @CacheEvict(value = "user:roles", key = "#dto.id", condition = "#dto != null && #dto.id != null")
    public boolean updateByDto(UserDTO dto) {
        boolean updated = super.updateByDto(dto);
        if (updated) {
            log.info("用户信息已更新，缓存已失效: userId={}", dto.getId());
        }
        return updated;
    }

    @Override
    public UserEntity getUserByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUsername, username));
    }

    /**
     * 获取用户角色键列表（带缓存）
     * <p>
     * 缓存策略：
     * <ul>
     * <li>缓存键：user:roles:{userId}</li>
     * <li>过期时间：30 分钟（由 CacheManager 配置）</li>
     * <li>失效时机：角色分配/移除时</li>
     * </ul>
     *
     * @param userId 用户ID
     * @return 角色键列表（带 ROLE_ 前缀）
     */
    @Cacheable(value = "user:roles", key = "#userId")
    public List<String> getUserRoleKeys(Long userId) {
        log.debug("从数据库查询用户角色: userId={}", userId);
        List<RoleEntity> roles = userMapper.selectRolesByUserId(userId);
        return roles.stream()
                .map(role -> "ROLE_" + role.getRoleKey())
                .collect(Collectors.toList());
    }

    /**
     * 失效用户角色缓存
     * <p>
     * 在角色分配、移除或用户删除时调用此方法失效缓存
     *
     * @param userId 用户ID
     */
    @CacheEvict(value = "user:roles", key = "#userId")
    public void evictUserRolesCache(Long userId) {
        log.info("失效用户角色缓存: userId={}", userId);
    }
}
