---
sidebar_position: 2
---

# 认证 API

用户认证相关接口，包括注册、登录和登出。

## 基础信息

**基础路径**: `/auth`  
**认证要求**: 仅 `/logout` 需要 Token  
**响应格式**: `Result<T>`

---

## POST /auth/register

用户注册接口

### 请求

**URL**: `POST /auth/register`  
**认证**: ❌ 公开接口  
**Content-Type**: `application/json`

**请求体** (`RegisterDTO`):

| 字段 | 类型 | 必填 | 说明 | 验证规则 |
|------|------|------|------|----------|
| username | String | ✅ | 用户名 | 3-20个字符，字母数字下划线 |
| password | String | ✅ | 密码 | 8-20个字符，包含大小写字母和数字 |
| email | String | ✅ | 邮箱 | 有效的邮箱格式 |
| nickname | String | ❌ | 昵称 | 最多30个字符 |

### 响应

**成功** (HTTP 200):

```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "id": 1,
    "username": "newuser",
    "email": "new@example.com",
    "nickname": "New User",
    "status": 1,
    "createdAt": "2025-12-10T14:30:00"
  }
}
```

**失败场景**:

| Code | 消息 | 原因 |
|------|------|------|
| 400 | 用户名已存在 | username 重复 |
| 400 | 邮箱已被注册 | email 重复 |
| 400 | 参数验证失败 | DTO 验证不通过 |

### 示例

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Password123!",
    "email": "test@example.com",
    "nickname": "Test User"
  }'
```

---

## POST /auth/login

用户登录接口

### 请求

**URL**: `POST /auth/login`  
**认证**: ❌ 公开接口  
**Content-Type**: `application/json`

**请求体** (`LoginDTO`):

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | ✅ | 用户名或邮箱 |
| password | String | ✅ | 密码 |

### 响应

**成功** (HTTP 200):

```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMTIzIiwiaWF0IjoxNzAyMTk...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": 1,
      "username": "testuser",
      "email": "test@example.com",
      "nickname": "Test User",
      "roles": ["USER"]
    }
  }
}
```

**响应字段说明**:

| 字段 | 说明 |
|------|------|
| token | JWT Token，用于后续API认证 |
| tokenType | Token类型，固定为 "Bearer" |
| expiresIn | Token有效期（秒），默认86400（24小时）|
| user | 用户基本信息 |

**失败场景**:

| Code | 消息 | 原因 |
|------|------|------|
| 401 | 用户名或密码错误 | 凭证不匹配 |
| 401 | 账号已被禁用 | user.status = 0 |

### 示例

```bash
# 使用用户名登录
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Password123!"
  }'

# 使用邮箱登录
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test@example.com",
    "password": "Password123!"
  }'
```

---

## POST /auth/logout

用户登出接口

### 请求

**URL**: `POST /auth/logout`  
**认证**: ✅ 需要 JWT Token  
**Content-Type**: `application/json`

**请求头**:

```http
Authorization: Bearer {token}
```

### 响应

**成功** (HTTP 200):

```json
{
  "code": 0,
  "message": "Success",
  "data": null
}
```

### 说明

> ⚠️ **重要提示**: 由于 JWT 是无状态的，登出主要由**客户端删除 Token** 实现。
> 
> 此接口仅用于：
> - 记录登出日志
> - 未来可扩展为 Token 黑名单机制

### 示例

```bash
curl -X POST http://localhost:8080/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## 使用流程

```mermaid
graph TD
    A[客户端] -->|1. 注册| B[POST /auth/register]
    B -->|返回 UserVO| C[注册成功]
    
    C -->|2. 登录| D[POST /auth/login]
    D -->|返回 Token| E[保存 Token 到本地]
    
    E -->|3. 访问受保护API| F[添加 Authorization Header]
    F -->|携带 Token| G[访问 /api/**]
    
    G -->|Token过期| H[POST /auth/login]
    H -->|刷新 Token| E
    
    G -->|4. 登出| I[POST /auth/logout]
    I -->|删除本地 Token| J[登出成功]
```

---

## 安全建议

1. **HTTPS**: 生产环境必须使用 HTTPS 传输
2. **Token 存储**: 
   - Web: 存储在 `httpOnly` Cookie 或 `sessionStorage`
   - 移动端: 存储在安全的 Keychain/Keystore
3. **Token 过期**: 默认 24 小时，可配置
4. **密码强度**: 前端应验证密码强度
5. **防暴力破解**: 建议实现登录失败限流

---

## 相关文档

- [用户管理 API](./users) - 用户信息查询和更新
- [Security 配置](../infrastructure/security/overview) - 三链架构详解
- JwtTokenProvider 文档 - JWT Token 管理工具
