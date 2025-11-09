[根目录](../CLAUDE.md) > **blog-admin-server**

# blog-admin-server 模块

## 模块职责
独立的 Spring Boot Admin 监控服务端，用于聚合展示所有微服务的健康状态和指标。提供统一的监控仪表盘和管理功能。

## 入口与启动

### 主启动类
```java
package com.blog;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableAdminServer
@SpringBootApplication
public class AdminServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminServerApplication.class, args);
    }
}
```

### 配置文件
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-admin-server/src/main/resources/application.yaml`: 应用配置文件
  - 服务器端口：9000 (默认)
  - Spring Security 配置（登录认证、CSRF忽略等）
  - Actuator 端点配置
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-admin-server/src/main/resources/logback-spring.xml`: Logback日志配置文件

## 对外接口
- Spring Boot Admin UI: http://localhost:9000/ (默认端口，需登录访问)
- 暴露 Actuator 端点用于自我监控（如 `/actuator/health`, `/actuator/info`）

## 关键依赖与配置

### 主要依赖
- Spring Boot Starter Web: Web应用基础。
- Spring Boot Admin Starter Server: Spring Boot Admin Server 核心依赖，提供监控服务端功能。
- Spring Boot Starter Security: 为 Admin Server 提供登录认证和访问控制。
- Spring Boot Starter Actuator: 提供自身应用的监控和管理端点。

### 安全配置 (示例)
```java
// /Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-admin-server/src/main/java/com/blog/config/SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler =
                new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl("/");

        http
            .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                .requestMatchers("/assets/**", "/login", "/actuator/**", "/instances/**") // 允许公共访问
                .permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(formLogin -> formLogin // 启用表单登录
                .loginPage("/login")
                .successHandler(successHandler)
            )
            .logout(logout -> logout // 启用注销
                .logoutUrl("/logout")
            )
            .httpBasic(withDefaults()) // 启用 HTTP Basic 认证
            .csrf(csrf -> csrf // 配置 CSRF
                .ignoringRequestMatchers("/instances/**", "/actuator/**") // 忽略对特定端点的CSRF检查
            );
        return http.build();
    }
}
```

## 数据模型
- 此模块不涉及业务数据模型，主要处理监控数据和应用状态。

## 测试与质量
- 暂无特定测试。建议添加针对安全配置和Admin Server启动的集成测试。

## 常见问题 (FAQ)

### Q: 如何更改Admin Server的端口？
A: 编辑 `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-admin-server/src/main/resources/application.yaml` 文件中的 `server.port` 配置。

### Q: 客户端如何注册到Admin Server？
A: 在客户端应用（如 `blog-application`）中添加 `spring-boot-admin-starter-client` 依赖，并在其 `application.yaml` 中配置 Admin Server 的地址：`spring.boot.admin.client.url=http://localhost:9000`。

## 相关文件清单
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-admin-server/pom.xml` - Maven配置
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-admin-server/src/main/java/com/blog/AdminServerApplication.java` - 主启动类
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-admin-server/src/main/java/com/blog/config/SecurityConfig.java` - Spring Security 配置
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-admin-server/src/main/resources/application.yaml` - 应用配置文件
- `/Users/liusx/CodeRepository/IdeaProjects/personal-blog-backend/blog-admin-server/src/main/resources/logback-spring.xml` - Logback日志配置文件

## 变更记录 (Changelog)

### 2025-11-09
- 架构师完成模块扫描，更新模块文档，补充关键依赖和相关文件清单，包含详细的配置和安全配置示例。

### 2025-09-19
- 创建模块文档
- 记录启动类和配置信息
