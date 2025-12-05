package com.blog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.blog.common.security.JwtAuthenticationDetails;
import com.blog.common.security.JwtTokenProvider;

/**
 * JWT 认证过滤器
 * <p>
 * 拦截请求，从 Authorization header 提取 JWT Token，验证后设置 Spring Security 上下文。
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.isNotBlank(jwt) && jwtTokenProvider.validateToken(jwt)) {
                authenticateUser(jwt, request);
            }
        } catch (Exception ex) {
            log.error("无法设置用户认证: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中提取 JWT Token
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.isNotBlank(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    /**
     * 认证用户并设置 Security Context
     */
    private void authenticateUser(String jwt, HttpServletRequest request) {
        String username = jwtTokenProvider.getUsernameFromToken(jwt);
        Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
        List<String> roles = jwtTokenProvider.getRolesFromToken(jwt);

        // 构造权限列表
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // 创建 UserDetails（这里不需要密码，因为已经通过 Token 验证）
        UserDetails userDetails = User.builder()
                .username(username)
                .password("") // Token 认证不需要密码
                .authorities(authorities)
                .build();

        // 创建认证对象
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
                authorities);

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // 将用户ID存储到认证对象的details中（用于 SecurityUtils 提取）
        JwtAuthenticationDetails details = new JwtAuthenticationDetails(request, userId);
        authentication.setDetails(details);

        // 设置到 Security Context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("已设置用户 '{}' (ID: {}) 的认证信息", username, userId);
    }
}
