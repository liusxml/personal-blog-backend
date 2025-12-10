---
sidebar_position: 3
---

# SpringDoc OpenAPI 使用指南

本文档基于 `personal-blog-backend` 项目的实际配置，详细说明如何在 Spring Boot 中集成和使用 `springdoc-openapi` 生成接口文档。

## 1. 简介

SpringDoc 是基于 OpenAPI 3 规范的 Spring Boot 接口文档生成工具。相比旧版 Swagger 2 (springfox)，它支持更丰富的注解、更好的响应式编程支持以及更现代的 UI。

**核心组件：**
- **springdoc-openapi-starter-webmvc-ui**: 自动生成文档并集成 Swagger UI。
- **OpenAPI 3 规范**: 业界通用的 API 描述标准。

## 2. 快速配置

### 2.1 依赖引入 (pom.xml)
在 `blog-common` 或具体的业务模块中引入：
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
</dependency>
```

### 2.2 基础配置 (application.yaml)
```yaml
springdoc:
  api-docs:
    path: /v3/api-docs                  # 接口描述文件路径 (JSON)
  swagger-ui:
    path: /swagger-ui.html              # UI 访问路径
    tags-sorter: alpha                  # 接口分组排序 (按字母)
    operations-sorter: alpha            # 接口方法排序 (按字母)
  packages-to-scan: com.blog            # 扫描包路径 (避免扫描框架接口)
  show-actuator: true                   # 是否显示 Actuator 监控端点
```

### 2.3 全局配置 (Java Bean)
在 `blog-application` 的 `com.blog.config.OpenApiConfig` 中配置了全局信息和安全认证：

```java title="blog-application/src/main/java/com/blog/config/OpenApiConfig.java"
@Configuration
public class OpenApiConfig {
    
    private static final String SECURITY_SCHEME_NAME = "BearerAuth";
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            // 1. 配置基本信息
            .info(new Info()
                .title("个人博客后台系统 API")
                .description("Personal Blog Backend API Documentation")
                .version("1.0.0")
                // 作者信息
                .contact(new Contact()
                        .name("liusxml")
                        .email("liusxml@example.com")
                        .url("https://github.com/liusxml"))
                // 许可证信息
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
            // 2. 配置全局的安全方案 (JWT)
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
            .components(new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME,
                    new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

:::tip 配置说明
- **Info**: 配置API标题、描述、版本、作者联系方式和许可证
- **SecurityScheme**: 配置JWT认证方案，使Swagger UI显示"Authorize"按钮
- **BearerAuth**: 使用常量管理安全方案名称，便于维护
:::

## 3. 开发注解指南

在 Controller 和 DTO 中无需使用旧版 `@Api` 或 `@ApiOperation`，请使用以下 OpenAPI 3 标准注解。

### 3.1 控制器类 (Controller)
使用 `@Tag` 对接口进行分组。

```java
@RestController
@RequestMapping("/auth")
@Tag(name = "认证管理", description = "用户注册、登录、登出等认证相关接口") // <--- 关键注解
public class AuthController { ... }
```

### 3.2 接口方法 (Method)
使用 `@Operation` 描述接口功能。

```java
@PostMapping("/login")
@Operation(summary = "用户登录", description = "使用用户名/邮箱和密码登录，返回JWT Token") // <--- 关键注解
public Result<LoginVO> login(@RequestBody LoginDTO loginDTO) { ... }
```

**常用属性：**
- `summary`: 简短摘要（显示在列表页）。
- `description`: 详细描述（点开后显示）。

### 3.3 参数模型 (DTO/VO)
使用 `@Schema` 描述实体类和字段。

```java
@Data
@Schema(description = "用户登录请求DTO")
public class LoginDTO {
    
    @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String username;

    @Schema(description = "密码", example = "123456")
    private String password;
}
```

## 4. 访问与安全

### 4.1 访问地址
- **接口文档 UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

### 4.2 权限控制
本项目支持两种 Swagger 访问模式（通过 `application.yaml` 控制）：

1.  **公开模式（当前）**：
    在 `app.security.permit-all-urls` 中加入了 Swagger 相关路径，无需登录即可查看文档。

2.  **安全模式（推荐）**：
    将 Swagger 路径从白名单移除。访问时会自动跳转到 **表单登录页**，需输入数据库中的管理员账号（如 `admin`/`123456`）才能查看。

### 4.3 接口调试
在 Swagger UI 中调试受保护接口（如修改用户信息）时：
1.  先调用 `/auth/login` 接口获取 `token`。
2.  点击页面顶部的 **Authorize** 按钮。
3.  输入 Token（不用加 `Bearer ` 前缀，SpringDoc 会自动处理）。
4.  点击 Authorize，之后的请求都会自动带上 Token。
