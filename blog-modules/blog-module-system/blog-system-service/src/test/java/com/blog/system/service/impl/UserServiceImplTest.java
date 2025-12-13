package com.blog.system.service.impl;

import com.blog.common.exception.BusinessException;
import com.blog.common.security.JwtTokenProvider;
import com.blog.system.api.dto.RegisterDTO;
import com.blog.system.api.vo.UserVO;
import com.blog.system.constant.RoleConstants;
import com.blog.system.converter.UserConverter;
import com.blog.system.entity.SysRole;
import com.blog.system.entity.SysUser;
import com.blog.system.mapper.RoleMapper;
import com.blog.system.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UserServiceImpl 单元测试
 * <p>
 * 覆盖核心业务逻辑：注册、登录、更新等
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserConverter userConverter;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // 注入配置属性 jwtExpiration
        org.springframework.test.util.ReflectionTestUtils.setField(userService, "jwtExpiration", 7200000L);
        // 注入父类 MybatisPlus 的 baseMapper (避免 updateByDto 报错)
        org.springframework.test.util.ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @Test
    @DisplayName("should_ReturnUserVo_When_RegisterSuccess: 验证用户注册成功流程")
    void should_ReturnUserVo_When_RegisterSuccess() {
        log.info("Testing: User Registration - Success Scenario");

        // Given: 准备注册数据
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("newuser");
        registerDTO.setPassword("password123");
        registerDTO.setEmail("newuser@example.com");
        registerDTO.setNickname("New User");
        log.info("Given: RegisterDTO prepared for username: {}", registerDTO.getUsername());

        // Mock: 用户名不存在 (selectOne 返回 null)
        when(userMapper.selectOne(argThat(wrapper -> {
            // 简单模拟: 只要调用 selectOne 且 wrapper sqlSegment 包含 username 就返回 null
            // 由于 MyBatis-Plus wrapper mock 较复杂，这里简化为 any() 并假设第一次是查用户名
            return true;
        }))).thenReturn(null);
        // 注意：实际代码中调用了两次 selectOne (一次 check username, 一次 check email)
        // 为了更精确 Mock，我们可以使用 doReturn(...).when(...) 配合具体的 ArgumentMatcher，
        // 但由于LambdaQueryWrapper是对象，equals比较困难。
        // 在单元测试中，我们通常假设只要 selectOne 返回 null 即可通过检查。

        // Mock: 密码加密
        when(passwordEncoder.encode(registerDTO.getPassword())).thenReturn("encodedPassword");

        // Mock: 默认角色查询 (SysRole)
        SysRole defaultRole = new SysRole();
        defaultRole.setId(1L);
        defaultRole.setRoleKey(RoleConstants.DEFAULT_USER_ROLE);
        when(roleMapper.selectOne(any())).thenReturn(defaultRole);

        // Mock: 插入用户 (返回1表示成功)
        when(userMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(100L); // 模拟落库生成ID
            return 1;
        });

        // Mock: 角色分配逻辑 (void方法，不做操作)
        // Mock: Converter 转换
        UserVO expectedVo = new UserVO();
        expectedVo.setId(100L);
        expectedVo.setUsername("newuser");
        when(userConverter.entityToVo(any(SysUser.class))).thenReturn(expectedVo);

        // When: 执行注册
        UserVO result = userService.register(registerDTO);
        log.info("When: userService.register called, result user ID: {}", result.getId());

        // Then: 验证结果
        assertNotNull(result, "Result UserVO should not be null");
        assertEquals(100L, result.getId(), "User ID should match generated ID");
        assertEquals("newuser", result.getUsername(), "Username should match");
        assertTrue(result.getRoles().contains("ROLE_USER"), "Should contain default role");

        // Verify: 验证关键方法调用次数
        verify(userMapper, times(2)).selectOne(any()); // 检查用户名 + 邮箱
        verify(passwordEncoder).encode("password123"); // 密码加密
        verify(userMapper).insert(any(SysUser.class)); // 插入用户
        verify(roleMapper).assignRoleToUser(100L, 1L); // 分配角色

        log.info("Then: Registration flow verified successfully.");
    }

    @Test
    @DisplayName("should_ThrowBusinessException_When_UsernameExists: 验证注册时用户名已存在抛出异常")
    void should_ThrowBusinessException_When_UsernameExists() {
        log.info("Testing: User Registration - Username Exists Scenario");

        // Given
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setUsername("existingUser");

        // Mock: 模拟数据库中已存在该用户
        SysUser existingUser = new SysUser();
        existingUser.setId(99L);
        existingUser.setUsername("existingUser");

        // 当 queryWrapper 查询时返回 existingUser
        when(userMapper.selectOne(any())).thenReturn(existingUser);
        log.info("Given: Existing user mocked in database");

        // When & Then: 预期抛出 BusinessException
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            userService.register(registerDTO);
        });

        log.info("When: register called. Then: Caught expected exception: {}", exception.getMessage());

        // 验证异常类型或错误码 (假设 SystemErrorCode 这里未直接暴露，验证消息或Code即可)
        // 这里简单验证不需要 insert 操作
        verify(userMapper, never()).insert(any(SysUser.class));
    }

    @Test
    @DisplayName("should_ReturnLoginVo_When_LoginSuccess: 验证用户登录成功流程")
    void should_ReturnLoginVo_When_LoginSuccess() {
        log.info("Testing: User Login - Success Scenario");

        // Given
        com.blog.system.api.dto.LoginDTO loginDTO = new com.blog.system.api.dto.LoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("password");

        SysUser mockUser = new SysUser();
        mockUser.setId(100L);
        mockUser.setUsername("testuser");
        mockUser.setPassword("encodedPassword");
        mockUser.setStatus(1); // 正常状态

        // Mock: 根据用户名查用户
        when(userMapper.selectOne(any())).thenReturn(mockUser);

        // Mock: 密码验证
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);

        // Mock: 角色查询 (用于 getUserRoleKeys)
        SysRole role = new SysRole();
        role.setRoleKey("USER");
        when(userMapper.selectRolesByUserId(100L)).thenReturn(List.of(role));

        // Mock: Token生成
        when(jwtTokenProvider.generateToken(any(), eq(100L))).thenReturn("mockToken");

        // Mock: Converter
        UserVO mockUserVO = new UserVO();
        mockUserVO.setId(100L);
        mockUserVO.setUsername("testuser");
        when(userConverter.entityToVo(mockUser)).thenReturn(mockUserVO);

        // When
        com.blog.system.api.vo.LoginVO result = userService.login(loginDTO);
        log.info("When: userService.login called");

        // Then
        assertNotNull(result);
        assertEquals("mockToken", result.getToken());
        assertEquals("testuser", result.getUser().getUsername());
        assertTrue(result.getUser().getRoles().contains("ROLE_USER"));
        log.info("Then: Login flow verified successfully.");
    }

    @Test
    @DisplayName("should_ThrowException_When_InvalidCredentials: 验证登录密码错误抛出异常")
    void should_ThrowException_When_InvalidCredentials() {
        log.info("Testing: User Login - Invalid Credentials");

        // Given
        com.blog.system.api.dto.LoginDTO loginDTO = new com.blog.system.api.dto.LoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("wrongPassword");

        SysUser mockUser = new SysUser();
        mockUser.setId(100L);
        mockUser.setPassword("encodedPassword");

        when(userMapper.selectOne(any())).thenReturn(mockUser);
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // When & Then
        assertThrows(BusinessException.class, () -> userService.login(loginDTO));
        log.info("Then: BusinessException thrown for invalid password");
    }

    @Test
    @MockitoSettings(strictness = Strictness.LENIENT)
    @DisplayName("should_UpdateUser_When_UpdateByDto: 验证用户更新及缓存清理")
    void should_UpdateUser_When_UpdateByDto() {
        log.info("Testing: User Update - Success Scenario");

        // Given
        com.blog.system.api.dto.UserDTO userDTO = new com.blog.system.api.dto.UserDTO();
        userDTO.setId(100L);
        userDTO.setNickname("Updated Nickname");

        SysUser mockUser = new SysUser();
        mockUser.setId(100L);
        lenient().when(userMapper.selectById(100L)).thenReturn(mockUser);

        when(userConverter.dtoToEntity(userDTO)).thenReturn(mockUser);
        when(userMapper.updateById(mockUser)).thenReturn(1);

        // When
        boolean result = userService.updateByDto(userDTO);

        // Then
        assertTrue(result);
        verify(userMapper).updateById(mockUser);
        // 注意: @CacheEvict 是 Spring AOP 行为，单元测试默认不生效，除非使用集成测试或特殊配置
        // 但我们可以验证业务逻辑是否执行
        log.info("Then: param update executed.");
    }
}
