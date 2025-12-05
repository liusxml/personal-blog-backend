# 🛡️ Personal Blog Backend - Security Guide

本文档详细说明了本项目的安全架构、JWT 认证机制以及相关功能的实现细节。

## 1. 核心安全架构

本项目基于 **Spring Security 6** 构建，采用 **模块化单体 (Modular Monolith)** 架构。为了兼顾前后端分离的用户认证和微服务架构下的机器监控，采用了 **双过滤链 (Dual Filter Chain)** 策略。

### 1.1 双过滤链设计

| 优先级 | 过滤链名称 | 匹配路径 | 认证方式 | 用途 |
| :--- | :--- | :--- | :--- | :--- |
| **High** (`@Order(2)`) | `jwtFilterChain` | `/api/**` | **JWT** | 前端用户访问业务接口，无状态 (Stateless)。 |
| **Low** (`@Order(3)`) | `defaultChain` | `/**` (剩余路径) | **HTTP Basic** | Spring Boot Admin 等监控系统访问 `/actuator/**` 端点。 |

### 1.2 模块化设计

安全组件根据职责被拆分到不同模块，严格遵守依赖倒置原则：

- **`blog-common`**: 存放通用工具和配置（服务模块依赖它）。
  - `JwtTokenProvider`: Token 生成与验证工具。
  - `SecurityProperties`: JWT 密钥与过期时间配置。
  - `SecurityUtils`: 获取当前登录用户的上下文工具。
  - `JwtAuthenticationDetails`: 用户详细信息数据类。
- **`blog-system-service`**: 存放用户、角色实体及登录逻辑（业务核心）。
  - `UserServiceImpl`: 登录时调用 `JwtTokenProvider` 生成 Token。
  - `DBUserDetailsServiceImpl`: 实现 `UserDetailsService` 加载数据库用户。
- **`blog-application`**: 存放应用层配置（组装层）。
  - `SecurityConfig`: 组装 SecurityFilterChain。
  - `JwtAuthenticationFilter`: 仅在 Web 入口处拦截请求。

---

## 2. JWT 认证与配置

### 2.1 配置参数

JWT 的相关配置统一管理在 `SecurityProperties` 类中，并通过 `application.yaml` 进行配置。

**配置文件路径**: `blog-application/src/main/resources/application.yaml`

```yaml
app:
  security:
    # 鉴权白名单 (无需登录即可访问)
    permit-all-urls:
      - "/v3/api-docs/**"
      - "/swagger-ui/**"
      - "/auth/login"
      - "/auth/register"

    # JWT 认证配置
    jwt-secret: dev-blog-jwt-secret-key-minimum-256-bits-change-in-production-environment
    # ⚠️ 生产环境建议通过环境变量注入: ${JWT_SECRET}
    
    jwt-expiration: 7200000                     # Token有效期 (ms), 默认 2小时
```

### 2.2 Token 生成与验证

- **生成**: 用户登录成功后，`UserServiceImpl` 调用 `JwtTokenProvider.generateToken(UserDetails)`，将用户名、用户ID及角色信息 (`roles`) 放入 Payload 中并签名。
- **验证**: `JwtAuthenticationFilter` 拦截所有 `/api/**` 请求，提取 `Authorization: Bearer <token>` 头。如果 Token 合法且未过期，将用户信息存入 `SecurityContextHolder`。

---

## 3. RBAC 权限模型

系统实现了标准的 **RBAC (Role-Based Access Control)** 模型，支持基于角色的权限控制。

### 3.1 数据库设计

- **`sys_user`**: 用户表（username, password, nickname...）
- **`sys_role`**: 角色表（role_name, role_key...）
- **`sys_user_role`**: 用户-角色关联表（多对多关系）

### 3.2 默认角色

初始化脚本 (`V1.0.1__init_system_data.sql`) 预置了以下角色：

| 角色名称 | 标识 (`role_key`) | 描述 |
| :--- | :--- | :--- |
| **管理员** | `ADMIN` | 拥有系统所有权限。 |
| **作者** | `AUTHOR` | 可发布和管理自己的文章。 |
| **用户** | `USER` | 普通注册用户，仅可浏览和评论。 |

### 3.3 权限控制使用

在 Controller 或 Service 层，可以使用 Spring Security 注解进行细粒度控制：

```java
// 仅管理员可访问
@PreAuthorize("hasRole('ADMIN')")
public Result<String> adminFunction() { ... }

// 拥有 user:view 权限（通常映射为角色能力）可访问
@PreAuthorize("hasAuthority('system:user:view')")
public Result<List<UserVO>> listUsers() { ... }
```

---

## 4. 开发指南

### 4.1 获取当前登录用户

在代码任意位置（Service/Controller/Component），使用 `SecurityUtils` 静态方法获取当前用户信息，**无需**在方法参数中传递 UserId。

```java
import com.blog.common.util.SecurityUtils;

public void createPost(PostDTO postDTO) {
    // 获取当前用户ID
    Long userId = SecurityUtils.getUserId();
    
    // 获取当前用户名
    String username = SecurityUtils.getUsername();
    
    // 检查是否有特定角色
    boolean isAdmin = SecurityUtils.isAdmin();
    
    // ... 业务逻辑
}
```

### 4.2 异常处理

安全相关的异常（如 Token 无效、无权限）会被统一拦截并转换为标准 JSON 响应：

- **401 Unauthorized**: 未提供 Token 或 Token 无效/过期。
- **403 Forbidden**: Token 有效但权限不足。

### 4.3 密码加密

所有用户密码在数据库中均以 **BCrypt** 哈希存储，严禁明文存储。
默认管理员账户: `admin` / `Admin@123`

---

## 5. 项目结构参考

```text
/blog-common/src/main/java/com/blog/common/
  ├── config/SecurityProperties.java      # [配置] JWT参数定义
  ├── security/JwtTokenProvider.java      # [核心] Token逻辑
  ├── security/JwtAuthenticationDetails.java
  └── util/SecurityUtils.java             # [工具] 上下文访问

/blog-application/src/main/java/com/blog/
  ├── config/SecurityConfig.java          # [配置] 过滤链组装
  └── security/JwtAuthenticationFilter.java # [Web] 认证过滤器

/blog-modules/blog-module-system/.../service/impl/
  └── DBUserDetailsServiceImpl.java       # [业务] 加载数据库用户
```
