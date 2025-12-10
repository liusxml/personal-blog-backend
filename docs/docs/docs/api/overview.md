---
sidebar_position: 1
---

# API 参考总览

Personal Blog Backend 提供完整的 RESTful API，基于 Spring Boot 3 + Spring Security 6 实现，使用 JWT Token 认证。

## 🔐 认证机制

所有API（除公开端点外）都需要 JWT Token 认证。

### 公开端点

| 路径 | 说明 |
|------|------|
| `POST /auth/register` | 用户注册 |
| `POST /auth/login` | 用户登录 |
| `GET /actuator/health` | 健康检查 |
| `/swagger-ui/**` | API 文档 |

### 认证流程

```mermaid
sequenceDiagram
    participant Client
    participant API
    participant Security
    participant Service

    Client->>API: POST /auth/login (username, password)
    API->>Service: 验证凭证
    Service->>API: 返回用户信息
    API->>Client: 201 + JWT Token

    Note over Client: 保存 Token

    Client->>API: GET /api/users/me<br/>Header: Authorization: Bearer {token}
    API->>Security: 验证 Token
    Security->>API: 解析用户信息
    API->>Service: 查询用户数据
    Service->>API: 返回数据
    API->>Client: 200 + UserVO
```

### 使用 Token

在所有受保护的请求中添加 Header：

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMTIzIi...
```

---

## 📚 API 分类

### 1. 认证管理 (Authentication)

基础路径: `/auth`

| 方法 | 路径 | 功能 | 认证 |
|------|------|------|------|
| POST | `/register` | 用户注册 | ❌ 公开 |
| POST | `/login` | 用户登录 | ❌ 公开 |
| POST | `/logout` | 用户登出 | ✅ 需要 |

[查看详细文档 →](./auth)

---

### 2. 用户管理 (Users)

基础路径: `/api/users`

| 方法 | 路径 | 功能 | 权限 |
|------|------|------|------|
| GET | `/me` | 获取当前用户信息 | User |
| PUT | `/me` | 更新个人资料 | User |
| GET | `/{id}` | 获取用户信息 | Admin |
| PUT | `/{id}` | 更新用户信息 | Admin |
| DELETE | `/{id}` | 删除用户 | Admin |

[查看详细文档 →](./users)

---

### 3. 角色管理 (Roles)

基础路径: `/api/roles`  
**权限要求**: 所有接口都需要 `ADMIN` 角色

| 方法 | 路径 | 功能 |
|------|------|------|
| POST | `/` | 创建角色 |
| GET | `/{id}` | 获取角色详情 |
| PUT | `/{id}` | 更新角色 |
| DELETE | `/{id}` | 删除角色 |
| POST | `/{roleId}/users/{userId}` | 为用户分配角色 |
| DELETE | `/{roleId}/users/{userId}` | 移除用户角色 |

[查看详细文档 →](./roles)

---

### 4. 文章模块 (Articles)

> ⚠️ **开发中** - 文章模块正在规划中，预计包含以下功能：
> - 文章CRUD操作
> - 文章分类管理
> - 文章标签管理
> - 文章发布/草稿状态管理

---

### 5. 评论模块 (Comments)

> ⚠️ **开发中** - 评论模块正在规划中，预计包含以下功能：
> - 评论CRUD操作
> - 多级评论/回复
> - 评论点赞功能
> - 评论审核功能

---

### 6. 文件管理 (Files)

> ⚠️ **开发中** - 文件模块基于 Bitiful S3 存储，预计包含以下功能：
> - 文件上传（支持大文件分片）
> - 预签名 URL 生成（前端直传）
> - 文件下载
> - 文件删除

[查看文件模块分析 →](../modules/file/analysis)

---

## 📊 统一响应格式

所有 API 都使用统一的 `Result<T>` 响应格式。

### 成功响应

```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com"
  },
  "timestamp": "2025-12-10T14:30:00"
}
```

### 错误响应

```json
{
  "code": 400,
  "message": "参数验证失败: 用户名不能为空",
  "data": null,
  "timestamp": "2025-12-10T14:30:00"
}
```

### 状态码说明

| Code | 含义 | 说明 |
|------|------|------|
| 0 | Success | 请求成功 |
| 400 | Bad Request | 参数错误或验证失败 |
| 401 | Unauthorized | 未登录或Token无效 |
| 403 | Forbidden | 无权限访问 |
| 404 | Not Found | 资源不存在 |
| 500 | Internal Server Error | 服务器内部错误 |

---

## 🛠️ 快速开始

### 1. 获取 Token

```bash
# 注册
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Password123!",
    "email": "test@example.com",
    "nickname": "Test User"
  }'

# 登录
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Password123!"
  }'

# 返回
{
  "code": 0,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": 1,
      "username": "testuser",
      "email": "test@example.com"
    }
  }
}
```

### 2. 使用 Token 访问 API

```bash
# 保存 Token
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

# 获取当前用户信息
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN"
```

---

## 📖 在线文档

访问 Swagger UI 查看完整的交互式 API 文档：

```
http://localhost:8080/swagger-ui.html
```

**功能亮点**：
- 🔍 完整的API列表和说明
- 🧪 在线测试（Try it out）
- 📋 请求/响应示例
- 🔐 JWT Token 认证支持

---

## 🔗 相关文档

- [快速上手](../getting-started/quick-start) - 环境搭建和第一个API
- [认证安全](../infrastructure/security/overview) - Security 三链架构详解
- [开发规范](../development/standards) - API 开发最佳实践
- [测试指南](../testing/overview) - API 测试方法

---

**最后更新**: 2025-12-10  
**API 版本**: v1.0  
**当前状态**: 🟢 系统模块已完成，文章/评论/文件模块开发中
