package com.blog.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.system.entity.SysRole;
import com.blog.system.entity.SysUser;
import com.blog.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security UserDetailsService 实现
 * <p>
 * 从数据库加载用户信息用于认证
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DBUserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("加载用户: {}", username);

        // 查询用户
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .or()
                        .eq(SysUser::getEmail, username) // 支持邮箱登录
        );

        if (user == null) {
            log.warn("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 查询用户角色
        List<SysRole> roles = userMapper.selectRolesByUserId(user.getId());

        // 构造权限列表 (添加 ROLE_ 前缀)
        List<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleKey()))
                .collect(Collectors.toList());

        // 构建 Spring Security User
        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .disabled(user.getStatus() == 0) // 0 表示禁用
                .authorities(authorities)
                .build();
    }
}
