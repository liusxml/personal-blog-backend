package com.blog.config;

import com.blog.common.config.SecurityProperties;
import com.blog.security.JwtAuthenticationFilter;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;
import java.util.Objects;

/**
 * <h2>Actuator 端点安全配置详解 (Spring Boot 3 + Spring Security 6)</h2>
 * <p>
 * 本类是 <strong>单体应用</strong> 中对 <code>/actuator/**</code> 端点的
 * <strong>精细化安全控制</strong> 实现。
 * 遵循以下核心原则：
 * <ul>
 * <li><strong>最小权限原则</strong>：仅放行必要端点，禁止默认全放行 <code>*</code></li>
 * <li><strong>多过滤链设计模式</strong>：使用 <code>@Order</code> 实现“白名单优先 →
 * 默认认证”双链架构，天然支持后期微服务拆分</li>
 * <li><strong>配置驱动</strong>：白名单路径从 <code>application.yaml</code> 动态注入，支持
 * profile 隔离</li>
 * <li><strong>无状态 API</strong>：禁用 CSRF + STATELESS，适配 RESTful 风格</li>
 * <li><strong>工具类规范</strong>：使用
 * <code>commons-lang3</code>、<code>commons-collections4</code>、<code>guava</code>
 * 进行防御性编程</li>
 * </ul>
 * <p>
 * <h3>架构设计考量（支持单体 → 微服务平滑演进）</h3>
 * <ul>
 * <li>当前：单体应用，所有端点共用同一安全链</li>
 * <li>未来：拆分为
 * <code>admin-service</code>、<code>gateway</code>、<code>business-service</code>
 * 时，<br>
 * 每个服务独立配置 <code>SecurityConfig</code>，仅需调整 <code>permitAllUrls</code> 即可</li>
 * <li>使用 <strong>策略模式</strong>（多链）而非单一链，避免后期“if-else 爆炸”</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 启用方法级安全注解（@PreAuthorize, @Secured等）
public class SecurityConfig {

    /**
     * 安全属性配置（从 application.yaml 绑定）
     */
    private final SecurityProperties securityProperties;

    /**
     * JWT 认证过滤器
     */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 构造函数注入安全属性，确保配置可测试性。
     *
     * @param securityProperties      安全配置属性（包含白名单路径）
     * @param jwtAuthenticationFilter JWT 认证过滤器
     */
    public SecurityConfig(SecurityProperties securityProperties,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.securityProperties = securityProperties;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // ===================================================================
    // 1. 白名单过滤链 (@Order(1)) - 最高优先级
    // ===================================================================

    /**
     * <h3>白名单路径安全过滤链</h3>
     * <p>
     * <strong>职责</strong>：对配置的 <code>permit-all-urls</code> 路径执行
     * <code>permitAll()</code>，<br>
     * 包括但不限于：
     * <ul>
     * <li><code>/actuator/health</code> - 健康检查（K8s、SBA 轮询必备）</li>
     * <li><code>/actuator/info</code> - 应用信息（SBA 显示）</li>
     * <li><code>/actuator/prometheus</code> - 指标采集</li>
     * <li><code>/v3/api-docs/**</code> - OpenAPI 文档（可选）</li>
     * </ul>
     * <p>
     * <strong>设计要点</strong>：
     * <ul>
     * <li>使用 <code>securityMatcher()</code> 精确匹配，避免影响其他路径</li>
     * <li>防御性编程：空列表时链不生效（避免 NPE）</li>
     * <li>使用 <code>ImmutableList</code> 保证线程安全</li>
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
                .cors(Customizer.withDefaults()) // 启用CORS
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    // ===================================================================
    // 2. JWT 认证过滤链 (@Order(2)) - API 访问控制
    // ===================================================================

    /**
     * <h3>JWT 认证过滤链</h3>
     * <p>
     * 匹配 <strong>/api/**</strong> 的请求，使用 JWT Token 认证。
     * <ul>
     * <li><code>/api/v1/auth/**</code> - 认证相关端点（注册、登录等）需要放行</li>
     * <li><code>/api/v1/**</code> - 业务 API，需要JWT认证</li>
     * </ul>
     * <p>
     * <strong>认证方式</strong>：JWT Bearer Token
     *
     * @param http HttpSecurity 构建器
     * @return 配置好的 JWT 过滤链
     * @throws Exception 配置异常
     */
    @Bean
    @Order(2)
    public SecurityFilterChain jwtChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**")
                .cors(Customizer.withDefaults()) // 启用CORS
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 认证相关接口 - 公开访问
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()

                        // 文章相关接口 - 公开访问（只读）
                        .requestMatchers("/api/v1/articles", "/api/v1/articles/*", "/api/v1/articles/*/related")
                        .permitAll()

                        // 评论查询接口 - 公开访问
                        .requestMatchers("/api/v1/comments/tree").permitAll()

                        // 其他 /api/** 需要认证
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // 添加 JWT 过滤器
        return http.build();
    }

    // ===================================================================
    // 3. 默认认证过滤链 (@Order(3)) - 兜底策略
    // ===================================================================

    /**
     * <h3>默认认证过滤链</h3>
     * <p>
     * 匹配 <strong>所有未被白名单和JWT链处理</strong> 的请求，强制要求认证。
     * <ul>
     * <li>包括：未放行的 actuator 端点（如 <code>/env</code>、<code>/beans</code>）</li>
     * </ul>
     * <p>
     * <strong>认证方式</strong>：HTTP Basic（适合机器间调用 + SBA 注册）
     *
     * @param http HttpSecurity 构建器
     * @return 配置好的默认认证链
     * @throws Exception 配置异常
     */
    @Bean
    @Order(3)
    public SecurityFilterChain defaultChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .formLogin(form -> form.defaultSuccessUrl("/swagger-ui.html", true)) // 启用默认表单登录并设置跳转
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    /**
     * 认证管理器配置
     * <p>
     * Spring Security 6 自动配置会查找 UserDetailsService 和 PasswordEncoder
     * 并组装成 DaoAuthenticationProvider。
     * <p>
     * 我们只需要在 Spring 容器中注册了 UserDetailsService (DBUserDetailsServiceImpl)
     * 和 PasswordEncoder (BCryptPasswordEncoder) 即可。
     */

    // ===================================================================
    // 私有工具方法
    // ===================================================================

    /**
     * <h3>获取并清洗白名单 URL (Stream API 重构版)</h3>
     * <p>
     * 使用 Java Stream API 对原始配置列表进行处理，实现：
     * <ul>
     * <li><strong>健壮性</strong>: 过滤掉 null 或空白字符串。</li>
     * <li><strong>标准化</strong>: 对每个 URL 执行 trim 并确保以 "/" 开头。</li>
     * <li><strong>不可变性</strong>: 最终结果收集到 Guava 的 ImmutableList 中。</li>
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
                // 2. 过滤掉空白字符串 (例如 "" 或 " ")
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