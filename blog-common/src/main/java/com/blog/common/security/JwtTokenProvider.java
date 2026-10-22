package com.blog.common.security;

import com.blog.common.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT Token 生成与验证工具类
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    /**
     * MCP 客户端专用长效 Token 的类型标识常量
     */
    public static final String TOKEN_TYPE_MCP = "MCP";

    private final SecurityProperties securityProperties;

    /**
     * 生成 JWT Token
     *
     * @param userDetails Spring Security 用户详情
     * @return JWT Token 字符串
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        // 提取角色信息
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        claims.put("roles", roles);

        return createToken(claims, userDetails.getUsername());
    }

    /**
     * 生成带有用户ID的 Token
     *
     * @param userDetails Spring Security 用户详情
     * @param userId      用户ID
     * @return JWT Token 字符串
     */
    public String generateToken(UserDetails userDetails, Long userId) {
        Map<String, Object> claims = new HashMap<>();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        claims.put("roles", roles);
        claims.put("userId", userId);

        return createToken(claims, userDetails.getUsername());
    }

    /**
     * 生成 MCP 客户端接入专用长效 JWT Token
     * <p>
     * 在标准登录 Claims（roles、userId）基础上额外写入 {@code tokenType = "MCP"} 标识与唯一 {@code jti}，
     * 并支持按管理员在后台选择的自定义有效时长（毫秒）签发长效凭证。
     *
     * @param userDetails      Spring Security 用户详情
     * @param userId           当前管理员用户ID
     * @param expirationMillis 自定义有效时长（毫秒）
     * @return MCP 专用 JWT Token 字符串
     */
    public String generateMcpToken(UserDetails userDetails, Long userId, long expirationMillis) {
        Map<String, Object> claims = new HashMap<>();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        claims.put("roles", roles);
        claims.put("userId", userId);
        claims.put("tokenType", TOKEN_TYPE_MCP);
        claims.put("jti", UUID.randomUUID().toString());

        return createToken(claims, userDetails.getUsername(), expirationMillis);
    }

    /**
     * 使用默认过期时间创建 Token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return createToken(claims, subject, securityProperties.getJwtExpiration());
    }

    /**
     * 使用指定过期时长创建 Token
     *
     * @param claims           JWT 载荷声明
     * @param subject          主题（用户名）
     * @param expirationMillis 有效时长（毫秒）
     * @return 签名后的 JWT 字符串
     */
    private String createToken(Map<String, Object> claims, String subject, long expirationMillis) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMillis))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 验证 Token 有效性
     *
     * @param token JWT Token
     * @return true 如果 Token 有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("JWT Token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 Token 中提取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * 从 Token 中提取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return (Long) userId;
    }

    /**
     * 从 Token 中提取角色列表
     *
     * @param token JWT Token
     * @return 角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return (List<String>) claims.get("roles");
    }

    /**
     * 从 Token 中提取凭证类型标识（如 MCP 专用凭证返回 "MCP"，普通登录凭证返回 null）
     *
     * @param token JWT Token
     * @return tokenType 字符串或 null
     */
    public String getTokenTypeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        Object tokenType = claims.get("tokenType");
        return tokenType != null ? tokenType.toString() : null;
    }

    /**
     * 从 Token 中提取签发时间（iat）
     *
     * @param token JWT Token
     * @return 签发时间 Date，若未设置则返回 null
     */
    public Date getIssuedAtFromToken(String token) {
        return getClaimsFromToken(token).getIssuedAt();
    }

    /**
     * 从 Token 中解析所有 Claims
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
