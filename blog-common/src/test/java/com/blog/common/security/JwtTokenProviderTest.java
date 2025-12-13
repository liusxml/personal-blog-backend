package com.blog.common.security;

import com.blog.common.config.SecurityProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * JwtTokenProvider 单元测试
 * 
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private SecurityProperties securityProperties;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private static final String SECRET_KEY = "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha-256";
    private static final Long EXPIRATION_TIME = 3600000L; // 1小时

    @BeforeEach
    void setUp() {
        // 初始化测试环境，模拟配置属性
        log.info("Test Setup: Mocking SecurityProperties with secret key and expiration.");
        // 使用 lenient() 避免 "Unnecessary stubbing" 错误 (因为某些测试用例可能因异常提前返回而未使用这些桩)
        lenient().when(securityProperties.getJwtSecret()).thenReturn(SECRET_KEY);
        lenient().when(securityProperties.getJwtExpiration()).thenReturn(EXPIRATION_TIME);
    }

    @Test
    @DisplayName("should_GenerateToken_When_UserDetailsValid: 验证生成包含正确Claims的Token")
    void should_GenerateToken_When_UserDetailsValid() {
        log.info("Testing: Token Generation with Valid UserDetails");

        // Given: 准备有效的用户信息 (ID: 100, Username: testuser, Role: ROLE_USER)
        Long userId = 100L;
        String username = "testuser";
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        UserDetails userDetails = new User(username, "password", authorities);
        log.info("Given: UserDetails created for user '{}' with id '{}'", username, userId);

        // When: 调用生成 Token 方法
        String token = jwtTokenProvider.generateToken(userDetails, userId);
        log.info("When: Generated token: {}", token);

        // Then: 验证 Token 生成结果
        // 1. Token 不为空
        assertNotNull(token, "Token should not be null");
        // 2. Token 格式符合 JWT 标准 (Header.Payload.Signature)
        assertEquals(3, token.split("\\.").length, "Token should have 3 parts (Header.Payload.Signature)");
        // 3. 能够从 Token 中正确解析出 UserID
        Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);
        assertEquals(userId, extractedUserId, "Extracted User ID should match the input");

        log.info("Then: Token generated and validated successfully.");
    }

    @Test
    @DisplayName("should_ReturnTrue_When_TokenValid: 验证有效Token")
    void should_ReturnTrue_When_TokenValid() {
        log.info("Testing: Validation of a Valid Token");

        // Given: 创建一个有效的 Token
        String token = createValidToken();
        log.info("Given: A valid token string");

        // When: 验证该 Token
        boolean isValid = jwtTokenProvider.validateToken(token);
        log.info("When: validateToken called, result: {}", isValid);

        // Then: 结果应为 true
        assertTrue(isValid, "Token should be valid");
        log.info("Then: Validation passed.");
    }

    @Test
    @DisplayName("should_ReturnFalse_When_TokenExpired: 验证过期Token")
    void should_ReturnFalse_When_TokenExpired() {
        log.info("Testing: Validation of an Expired Token");

        // Given: 模拟配置返回负的过期时间，使生成的 Token 立即过期
        when(securityProperties.getJwtExpiration()).thenReturn(-1000L);
        String expiredToken = createValidToken();
        log.info("Given: An expired token generated (expiration set to -1000ms)");

        // 注意：validateToken 校验依赖 Token 内部 Claims 的 exp 字段
        // 生成 Token 时已经写入了“过去”的时间作为 exp

        // When: 验证过期 Token
        boolean isValid = jwtTokenProvider.validateToken(expiredToken);
        log.info("When: validateToken called, result: {}", isValid);

        // Then: 结果应为 false
        assertFalse(isValid, "Expired token should be invalid");
        log.info("Then: Validation correctly failed for expired token.");
    }

    @Test
    @DisplayName("should_ReturnFalse_When_TokenMalformed: 验证篡改Token")
    void should_ReturnFalse_When_TokenMalformed() {
        log.info("Testing: Validation of a Malformed Token");

        // Given: 一个非法的 Token 字符串
        String invalidToken = "invalid.token.string";
        log.info("Given: A malformed token string: {}", invalidToken);

        // When: 验证该 Token
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);
        log.info("When: validateToken called, result: {}", isValid);

        // Then: 结果应为 false
        assertFalse(isValid, "Malformed token should be invalid");
        log.info("Then: Validation correctly failed for malformed token.");
    }

    @Test
    @DisplayName("should_ExtractCorrectClaims_When_TokenValid: 验证提取用户名和角色")
    void should_ExtractCorrectClaims_When_TokenValid() {
        log.info("Testing: Extraction of Claims from Valid Token");

        // Given: 创建一个有效的 Token
        String token = createValidToken();

        // When: 提取用户名和角色
        String username = jwtTokenProvider.getUsernameFromToken(token);
        List<String> roles = jwtTokenProvider.getRolesFromToken(token);
        log.info("When: Extracted username: '{}', roles: {}", username, roles);

        // Then: 验证提取的信息是否正确
        assertEquals("testuser", username, "Username should match");
        assertNotNull(roles, "Roles should not be null");
        assertTrue(roles.contains("ROLE_USER"), "Roles should contain ROLE_USER");
        log.info("Then: Extraction verification passed.");
    }

    private String createValidToken() {
        Long userId = 100L;
        String username = "testuser";
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        UserDetails userDetails = new User(username, "password", authorities);
        return jwtTokenProvider.generateToken(userDetails, userId);
    }
}
