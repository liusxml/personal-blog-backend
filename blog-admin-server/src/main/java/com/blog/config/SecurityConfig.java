package com.blog.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Spring Boot Admin Server 的 Spring Security 安全配置。
 * <p>
 * 这个配置类专门为 Spring Boot Admin Server 服务端定制安全策略，解决了
 * 默认 Spring Security 配置下可能出现的 CSRF 和 Thymeleaf 渲染问题。
 *
 * @author liusxml
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    /**
     * Spring Boot Admin Server 的上下文路径，例如 "/admin"。
     * 我们从 AdminServerProperties 中动态获取，以确保配置的一致性。
     */
    private final String adminContextPath;

    /**
     * 通过构造函数注入 AdminServerProperties，从中获取上下文路径。
     *
     * @param adminServerProperties Spring Boot Admin Server 的核心配置属性
     */
    public SecurityConfig(AdminServerProperties adminServerProperties) {
        this.adminContextPath = adminServerProperties.getContextPath();
    }

    /**
     * 配置安全过滤链 (SecurityFilterChain)，定义所有 HTTP 请求的访问规则。
     *
     * @param http HttpSecurity 对象，用于构建安全配置
     * @return 构建好的 SecurityFilterChain
     * @throws Exception 配置过程中可能抛出的异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(adminContextPath + "/");
        http
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers(
                                        adminContextPath + "/assets/**",
                                        adminContextPath + "/login",
                                        // 对客户端暴露的 actuator 端点也需要在这里放行
                                        "/actuator/**"
                                ).permitAll()
                                .anyRequest().authenticated()
                )
                .formLogin(formLogin ->
                        formLogin
                                .loginPage(adminContextPath + "/login")
                                .successHandler(successHandler)
                )
                .logout(logout ->
                        logout
                                .logoutUrl(adminContextPath + "/logout")
                                .logoutSuccessUrl(adminContextPath + "/login?logout")
                )
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // 【API 优化】使用新版 API 忽略 CSRF
                        .ignoringRequestMatchers(
                                adminContextPath + "/instances",
                                "/actuator/**", "/logout")
                );
        return http.build();
    }
}