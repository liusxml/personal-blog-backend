package com.blog.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

/**
 * 博客后端 JWT 认证令牌管理器
 *
 * <p><b>核心作用</b>：</p>
 * <ul>
 *   <li>当 AI 调用需要管理员权限的接口（如创建文章、发布文章）时，自动向 {@code POST /api/v1/auth/login} 发起登录请求获取 JWT Token。</li>
 *   <li>在内存中缓存获取到的 Token 及其过期时间，在有效期内直接复用，避免每次工具调用都重复登录。</li>
 * </ul>
 *
 * <p><b>为什么在 MCP Server 内部管理 Token？</b><br>
 * 外部大模型不应该知晓管理员的账号密码（安全隔离），也不需要理解后端的 JWT 刷新逻辑。
 * 由 MCP Server 在底层透明地附加 {@code Authorization: Bearer <token>} 请求头，对大模型做到完全无感。</p>
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

    @Value("${blog.api.password:admin123}")
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
     * <p>采用 {@code synchronized} 保证多线程并发调用时不会同时触发多次登录。</p>
     *
     * @return 完整的 Bearer Token 字符串
     */
    public synchronized String getBearerToken() {
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
            if (code != 200) {
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
