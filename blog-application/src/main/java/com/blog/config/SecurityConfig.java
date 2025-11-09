package com.blog.config;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Objects;

/**
 * <h2>Actuator 端点安全配置详解 (Spring Boot 3 + Spring Security 6)</h2>
 * <p>
 * 本类是 <strong>单体应用</strong> 中对 <code>/actuator/**</code> 端点的 <strong>精细化安全控制</strong> 实现。
 * 遵循以下核心原则：
 * <ul>
 *   <li><strong>最小权限原则</strong>：仅放行必要端点，禁止默认全放行 <code>*</code></li>
 *   <li><strong>多过滤链设计模式</strong>：使用 <code>@Order</code> 实现“白名单优先 → 默认认证”双链架构，天然支持后期微服务拆分</li>
 *   <li><strong>配置驱动</strong>：白名单路径从 <code>application.yaml</code> 动态注入，支持 profile 隔离</li>
 *   <li><strong>无状态 API</strong>：禁用 CSRF + STATELESS，适配 RESTful 风格</li>
 *   <li><strong>工具类规范</strong>：使用 <code>commons-lang3</code>、<code>commons-collections4</code>、<code>guava</code> 进行防御性编程</li>
 * </ul>
 * <p>
 * <h3>架构设计考量（支持单体 → 微服务平滑演进）</h3>
 * <ul>
 *   <li>当前：单体应用，所有端点共用同一安全链</li>
 *   <li>未来：拆分为 <code>admin-service</code>、<code>gateway</code>、<code>business-service</code> 时，<br>
 *       每个服务独立配置 <code>SecurityConfig</code>，仅需调整 <code>permitAllUrls</code> 即可</li>
 *   <li>使用 <strong>策略模式</strong>（多链）而非单一链，避免后期“if-else 爆炸”</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 安全属性配置（从 application.yaml 绑定）
     */
    private final SecurityProperties securityProperties;

    /**
     * 构造函数注入安全属性，确保配置可测试性。
     *
     * @param securityProperties 安全配置属性（包含白名单路径）
     */
    public SecurityConfig(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }


    // ===================================================================
    // 1. 白名单过滤链 (@Order(1)) - 最高优先级
    // ===================================================================

    /**
     * <h3>白名单路径安全过滤链</h3>
     * <p>
     * <strong>职责</strong>：对配置的 <code>permit-all-urls</code> 路径执行 <code>permitAll()</code>，<br>
     * 包括但不限于：
     * <ul>
     *   <li><code>/actuator/health</code> - 健康检查（K8s、SBA 轮询必备）</li>
     *   <li><code>/actuator/info</code> - 应用信息（SBA 显示）</li>
     *   <li><code>/actuator/prometheus</code> - 指标采集</li>
     *   <li><code>/v3/api-docs/**</code> - OpenAPI 文档（可选）</li>
     * </ul>
     * <p>
     * <strong>设计要点</strong>：
     * <ul>
     *   <li>使用 <code>securityMatcher()</code> 精确匹配，避免影响其他路径</li>
     *   <li>防御性编程：空列表时链不生效（避免 NPE）</li>
     *   <li>使用 <code>ImmutableList</code> 保证线程安全</li>
     * </ul>
     *
     * @param http HttpSecurity 构建器
     * @return 配置好的白名单过滤链
     * @throws Exception 配置异常
     */
    @Bean
    @Order(1)
    public SecurityFilterChain permitAllChain(HttpSecurity http) throws Exception {
        List<String> urls = getSafePermitAllUrls();
        if (CollectionUtils.isEmpty(urls)) {
            http.securityMatcher(r -> false);
            return http.build();
        }
        http.securityMatcher(urls.toArray(new String[0]))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    // ===================================================================
    // 2. 默认认证过滤链 (@Order(2)) - 兜底策略
    // ===================================================================

    /**
     * <h3>默认认证过滤链</h3>
     * <p>
     * 匹配 <strong>所有未被白名单链处理</strong> 的请求，强制要求认证。
     * <ul>
     *   <li>包括：未放行的 actuator 端点（如 <code>/env</code>、<code>/beans</code>）</li>
     *   <li>包括：业务 API（如 <code>/api/**</code>）</li>
     * </ul>
     * <p>
     * <strong>认证方式</strong>：HTTP Basic（适合机器间调用 + SBA 注册）
     *
     * @param http HttpSecurity 构建器
     * @return 配置好的默认认证链
     * @throws Exception 配置异常
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails adminUser = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("123456"))
                .roles("ADMIN", "USER")
                .build();

        UserDetails normalUser = User.builder()
                .username("user")
                .password(passwordEncoder.encode("123456"))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(adminUser, normalUser);
    }

    // ===================================================================
    // 私有工具方法
    // ===================================================================

    /**
     * <h3>获取并清洗白名单 URL (Stream API 重构版)</h3>
     * <p>
     * 使用 Java Stream API 对原始配置列表进行处理，实现：
     * <ul>
     *   <li><strong>健壮性</strong>: 过滤掉 null 或空白字符串。</li>
     *   <li><strong>标准化</strong>: 对每个 URL 执行 trim 并确保以 "/" 开头。</li>
     *   <li><strong>不可变性</strong>: 最终结果收集到 Guava 的 ImmutableList 中。</li>
     * </ul>
     * 该实现取代了传统的 for 循环和已废弃的 StringUtils API，代码更简洁、表达力更强。
     *
     * @return 一个线程安全的、经过清洗和标准化的 URL 列表。
     */
    private List<String> getSafePermitAllUrls() {
        List<String> rawUrls = securityProperties.getPermitAllUrls();
        if (CollectionUtils.isEmpty(rawUrls)) {
            return ImmutableList.of();
        }
        return rawUrls.stream()
                // 1. 过滤掉 null 元素 (防御性编程)
                .filter(Objects::nonNull)
                // 2. 过滤掉空白字符串 (例如 "" 或 "   ")
                .filter(StringUtils::isNotBlank)
                // 3. 对每个 URL 进行前后去空格
                .map(StringUtils::trim)
                // 4. 确保每个 URL 都以 "/" 开头，替代已废弃的 prependIfMissing
                .map(this::ensureLeadingSlash)
                // 5. 收集结果到一个不可变的列表中，保证线程安全
                .collect(ImmutableList.toImmutableList());
    }

    /**
     * 确保给定的路径字符串以 "/" 开头。
     *
     * @param path 原始路径
     * @return 以 "/" 开头的路径
     */
    private String ensureLeadingSlash(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }
}