package com.blog.security;

import com.blog.common.constants.CacheKeys;
import com.blog.common.security.JwtAuthenticationDetails;
import com.blog.common.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
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
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 认证过滤器
 * <p>
 * 拦截请求，从 Authorization header 提取 JWT Token，验证后设置 Spring Security 上下文。
 * 同时对 {@code tokenType = "MCP"} 的长效凭证执行最小权限路径隔离与 Redis 吊销状态检查。
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
    private final StringRedisTemplate stringRedisTemplate;

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
        String tokenType = jwtTokenProvider.getTokenTypeFromToken(jwt);

        // 若为 MCP 专用长效 Token，强制执行最小权限路径白名单与 Redis 吊销检查
        if (JwtTokenProvider.TOKEN_TYPE_MCP.equals(tokenType)) {
            if (!isAllowedMcpRequest(request)) {
                log.warn("拒绝 MCP 专用 Token 越权访问非授权端点: user={}, method={}, uri={}",
                        username, request.getMethod(), request.getRequestURI());
                return;
            }
            if (isMcpTokenRevoked(userId, jwt)) {
                log.warn("拒绝已吊销的 MCP 专用 Token 访问: user={}, userId={}", username, userId);
                return;
            }
        }

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

    /**
     * 校验 MCP 专用 Token 是否允许访问当前请求路径与 HTTP 方法
     * <p>
     * 仅放行：
     * <ul>
     *   <li>MCP 协议端点：{@code /mcp}、{@code /mcp/**}、{@code /sse}、{@code /sse/**}</li>
     *   <li>MCP 工具内部回环只读端点：{@code GET /api/v1/categories/**}、{@code GET /api/v1/tags/**}</li>
     *   <li>MCP 工具内部回环文章端点：{@code GET/POST/PUT /api/v1/admin/articles/**}（严禁 DELETE 删文章及访问其他管理接口）</li>
     * </ul>
     *
     * @param request 当前 HTTP 请求
     * @return true 表示属于 MCP 允许的最小权限范围
     */
    private boolean isAllowedMcpRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 1. MCP Streamable HTTP / SSE 协议端点
        if ("/mcp".equals(uri) || uri.startsWith("/mcp/") || "/sse".equals(uri) || uri.startsWith("/sse/")) {
            return true;
        }

        // 2. MCP 内部回环调用的分类与标签查询接口（仅允许 GET）
        if ("GET".equalsIgnoreCase(method)
                && ("/api/v1/categories".equals(uri) || uri.startsWith("/api/v1/categories/")
                || "/api/v1/tags".equals(uri) || uri.startsWith("/api/v1/tags/"))) {
            return true;
        }

        // 3. MCP 内部回环调用的管理端文章查询、保存与发布接口（仅允许 GET / POST / PUT，禁止 DELETE）
        if (("/api/v1/admin/articles".equals(uri) || uri.startsWith("/api/v1/admin/articles/"))
                && ("GET".equalsIgnoreCase(method)
                || "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method))) {
            return true;
        }

        return false;
    }

    /**
     * 检查用户的 MCP Token 是否已被吊销
     * <p>
     * 比对 Token 的签发时间（iat）与 Redis 中记录的该用户吊销基准时间戳 {@code mcp:token:revoked_before:{userId}}。
     *
     * @param userId 用户ID
     * @param jwt    JWT 字符串
     * @return true 表示已被吊销，应拒绝认证
     */
    private boolean isMcpTokenRevoked(Long userId, String jwt) {
        if (userId == null || stringRedisTemplate == null) {
            return false;
        }
        try {
            String revokedBeforeStr = stringRedisTemplate.opsForValue()
                    .get(CacheKeys.mcpTokenRevokedBeforeKey(userId));
            if (StringUtils.isBlank(revokedBeforeStr)) {
                return false;
            }
            long revokedBeforeMillis = Long.parseLong(revokedBeforeStr);
            Date issuedAt = jwtTokenProvider.getIssuedAtFromToken(jwt);
            // JJWT 的 iat 精度为秒（毫秒位截断为 000），若签发时间早于吊销基准时间则判定为已吊销
            return issuedAt == null || issuedAt.getTime() < revokedBeforeMillis;
        } catch (Exception e) {
            log.warn("读取 MCP Token 吊销状态异常: userId={}, error={}", userId, e.getMessage());
            return false;
        }
    }
}
