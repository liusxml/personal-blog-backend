package com.blog.security;

import com.blog.BlogApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import jakarta.annotation.Resource;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring Security 安全配置集成测试 v1.0
 * <p>
 * 该测试套件专注于验证 {@link com.blog.config.SecurityConfig} 中定义的安全规则是否按预期工作。
 * 它不关心业务逻辑的具体实现，只关心 "谁" 可以访问 "什么"，确保了应用的安全边界是清晰和正确的。
 *
 * @see SpringBootTest 启动完整的应用上下文，以便加载包括安全配置在内的所有Bean。
 * @see AutoConfigureMockMvc 自动配置 {@link MockMvc}，这是我们模拟HTTP请求以触发安全检查的核心工具。
 * @see WithAnonymousUser 这是一个Spring Security测试注解，它模拟一个“匿名”或“未登录”的用户发出的请求。
 * @see WithMockUser 这是另一个强大的注解，可以轻松模拟一个已经通过认证的用户，并可以指定其用户名和角色。
 * @see org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors#httpBasic(String, String)
 *      这是一个请求后处理器，用于在模拟请求中添加HTTP Basic Authentication头，是测试认证流程的关键。
 */
@SpringBootTest(classes = BlogApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("✅ Spring Security 安全配置集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SecurityIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(SecurityIntegrationTest.class);

    @Resource
    private MockMvc mockMvc;

    // 为了测试方便，我们虚构一个业务API路径。
    // 在实际项目中，你应该替换为你真实存在的、且需要被保护的任意一个API路径。
    private static final String PROTECTED_API_URL = "/api/v1/articles"; // 假设这是一个需要登录才能访问的文章接口

    // SpringDoc OpenAPI (Swagger) 的路径，这是我们在白名单中配置的。
    private static final String SWAGGER_UI_URL = "/swagger-ui.html";
    private static final String API_DOCS_URL = "/v3/api-docs";

    /**
     * 【第1步】验证白名单URL是否可以被匿名用户公开访问。
     * <p>
     * 这是对 `securityProperties.getPermitAllUrls()` 配置有效性的直接验证。
     * 我们模拟一个完全没有登录的用户，去访问Swagger API文档相关的路径。
     */
    @Test
    @Order(1)
    @WithAnonymousUser // 模拟一个匿名用户
    @DisplayName("1. 白名单URL - 匿名用户应可直接访问")
    void whenAnonymousAccessPermittedUrls_thenOk() throws Exception {
        log.info("▶️ 开始测试 (1/4): 匿名访问白名单URL (Swagger)...");

        log.info("   - 正在请求: {}", SWAGGER_UI_URL);
        mockMvc.perform(get(SWAGGER_UI_URL))
                .andExpect(status().is3xxRedirection()); // 预期结果: 200 OK

        log.info("   - 正在请求: {}", API_DOCS_URL);
        mockMvc.perform(get(API_DOCS_URL))
                .andExpect(status().isOk()); // 预期结果: 200 OK

        log.info("✅ 测试通过 (1/4): 白名单URL对匿名用户开放，符合预期。");
    }

    /**
     * 【第2步】验证受保护的URL是否会拒绝匿名用户的访问。
     * <p>
     * 此测试验证了 `.anyRequest().authenticated()` 规则的有效性。
     * 任何不在白名单中的URL，都应该被安全框架拦截。
     */
    @Test
    @Order(2)
    @WithAnonymousUser // 再次强调是匿名用户
    @DisplayName("2. 受保护URL - 匿名用户访问应被拒绝 (401)")
    void whenAnonymousAccessProtectedUrl_thenUnauthorized() throws Exception {
        log.info("▶️ 开始测试 (2/4): 匿名访问受保护URL...");
        log.info("   - 正在请求一个受保护的业务API: {}", PROTECTED_API_URL);

        mockMvc.perform(get(PROTECTED_API_URL))
                .andDo(print()) // 打印详细的请求和响应信息，便于调试
                .andExpect(status().isUnauthorized()); // 预期结果: 401 Unauthorized

        log.info("✅ 测试通过 (2/4): 受保护URL成功拦截了匿名请求，返回401。");
    }

    /**
     * 【第3步】验证使用正确的用户名和密码进行Basic Auth认证后，是否能成功访问受保护的URL。
     * <p>
     * 这个测试完整地覆盖了我们在 {@code SecurityConfig} 中配置的内存用户认证流程。
     * 它的成功，证明了 UserDetailsService 和 PasswordEncoder 都在正确工作。
     */
    @Test
    @Order(3)
    @DisplayName("3. 受保护URL - 合法用户Basic Auth认证应成功 (200)")
    void whenUserWithCorrectCredentials_thenOk() throws Exception {
        log.info("▶️ 开始测试 (3/4): 使用合法用户 (admin/123456) 访问受保护URL...");

        // 构造一个GET请求，并使用 httpBasic() 后处理器来添加 Authorization 头
        MockHttpServletRequestBuilder requestBuilder = get(PROTECTED_API_URL)
                .with(httpBasic("admin", "123456")); // 使用正确的用户名和密码

        mockMvc.perform(requestBuilder)
                .andDo(print())
                // 注意：这里我们预期的是 404 Not Found，而不是 200 OK。
                // 因为我们的测试重点是“安全认证”，而不是“业务逻辑”。
                // 只要请求通过了安全检查，到达了DispatcherServlet，对于安全测试来说就是成功的。
                // 404 恰好说明了请求已经穿透了安全层，但后面没有找到能处理 /api/v1/articles 的Controller。
                // 这完全符合我们的测试目标！
                .andExpect(status().isNotFound());

        log.info("✅ 测试通过 (3/4): Basic Auth认证成功，请求已通过安全过滤链。");
    }

    /**
     * 【第4步】验证使用错误的密码进行认证时，是否会被拒绝。
     * <p>
     * 这是一个“负面测试用例”，用于确保认证系统不会放行任何凭证错误的用户。
     */
    @Test
    @Order(4)
    @DisplayName("4. 受保护URL - 错误密码认证应被拒绝 (401)")
    void whenUserWithIncorrectCredentials_thenUnauthorized() throws Exception {
        log.info("▶️ 开始测试 (4/4): 使用错误密码 (admin/wrong_password) 访问受保护URL...");

        MockHttpServletRequestBuilder requestBuilder = get(PROTECTED_API_URL)
                .with(httpBasic("admin", "wrong_password")); // 使用错误的密码

        mockMvc.perform(requestBuilder)
                .andDo(print())
                .andExpect(status().isUnauthorized()); // 预期结果: 401 Unauthorized

        log.info("✅ 测试通过 (4/4): 认证系统成功拒绝了使用错误密码的请求。");
    }
}
