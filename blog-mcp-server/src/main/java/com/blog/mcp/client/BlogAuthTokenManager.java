package com.blog.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Map;

/**
 * 博客后端 JWT 认证令牌管理器
 *
 * <p><b>核心作用</b>：</p>
 * <ul>
 *   <li><b>优先透传请求头（推荐）</b>：当外部 MCP 客户端携带 {@code Authorization: Bearer <token>} 访问 {@code /mcp} 时，
 *       直接从当前 HTTP 请求上下文提取并复用该管理员 Token，无需依赖配置文件中的明文账号密码。</li>
 *   <li><b>账号密码登录兜底</b>：若非 HTTP 请求线程或未携带请求头，则向 {@code POST /api/v1/auth/login} 发起登录获取 JWT Token 并缓存。</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlogAuthTokenManager {

    /**
     * 提前刷新缓冲时间（300 秒 = 5 分钟），防止 Token 在请求临界点过期
     */
    private static final long EXPIRY_BUFFER_SECONDS = 300L;

    private final RestClient blogRestClient;
    private final ObjectMapper objectMapper;

    @Value("${blog.api.username:admin}")
    private String username;

    @Value("${blog.api.password:Admin@123}")
    private String password;

    /**
     * 内存缓存的 JWT Token 字符串
     */
    private volatile String cachedToken;

    /**
     * Token 的绝对过期时间点
     */
    private volatile Instant tokenExpireAt = Instant.MIN;

    /**
     * 获取合法的 Authorization 请求头值（格式：{@code Bearer <token>}）
     *
     * <p>优先从当前 HTTP 请求头提取客户端传入的 Bearer Token；若不存在则走内存缓存或账号密码登录兜底。</p>
     *
     * @return 完整的 Bearer Token 字符串
     */
    public synchronized String getBearerToken() {
        // 0. 优先从当前 MCP HTTP 请求上下文提取客户端传入的 Authorization: Bearer <token>
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                return authHeader;
            }
        }

        // 如果已缓存且未临近过期，直接返回复用
        if (StringUtils.hasText(cachedToken) && Instant.now().isBefore(tokenExpireAt)) {
            return "Bearer " + cachedToken;
        }

        log.info("MCP 认证管理器: 正在登录博客后端获取新 JWT Token, username={}", username);

        try {
            // 1. 调用博客后端的登录接口 POST /api/v1/auth/login
            String responseBody = blogRestClient.post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "username", username,
                            "password", password
                    ))
                    .retrieve()
                    .body(String.class);

            // 2. 解析统一响应体 Result<LoginVO> 中的 data.token 和 data.expiresIn
            JsonNode root = objectMapper.readTree(responseBody);
            int code = root.path("code").asInt(-1);
            if (code != 0 && code != 200) {
                String msg = root.path("message").asText("未知登录错误");
                throw new IllegalStateException("博客后端登录失败: " + msg);
            }

            JsonNode dataNode = root.path("data");
            String token = dataNode.path("token").asText();
            long expiresIn = dataNode.path("expiresIn").asLong(7200L);

            if (!StringUtils.hasText(token)) {
                throw new IllegalStateException("登录响应中未包含有效的 JWT Token");
            }

            // 3. 更新缓存与过期时间（扣除 5 分钟安全缓冲期）
            this.cachedToken = token;
            long validSeconds = Math.max(60L, expiresIn - EXPIRY_BUFFER_SECONDS);
            this.tokenExpireAt = Instant.now().plusSeconds(validSeconds);

            log.info("MCP 认证管理器: JWT Token 获取成功，有效期 {} 秒", validSeconds);
            return "Bearer " + this.cachedToken;

        } catch (Exception e) {
            log.error("MCP 认证管理器: 获取 JWT Token 异常", e);
            throw new IllegalStateException("无法获取博客管理员认证 Token: " + e.getMessage(), e);
        }
    }
}
