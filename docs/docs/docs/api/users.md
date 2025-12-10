---
sidebar_position: 3
---

# 用户管理 API

用户信息查询和管理接口。

## 基础信息

**基础路径**: `/api/users`  
**认证要求**: ✅ 所有接口都需要 JWT Token  
**权限要求**: 部分接口需要 `ADMIN` 角色  
**响应格式**: `Result<T>`

---

## GET /api/users/me

获取当前登录用户信息

### 请求

**URL**: `GET /api/users/me`  
**认证**: ✅ 需要 Token  
**权限**: User (所有登录用户)

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
  "data": {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "nickname": "Test User",
    "avatar": null,
    "status": 1,
    "roles": ["USER"],
    "createdAt": "2025-12-01T10:00:00",
    "updatedAt": "2025-12-10T14:30:00"
  }
}
```

### 示例

```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer {token}"
```

---

## PUT /api/users/me

更新当前用户个人资料

### 请求

**URL**: `PUT /api/users/me`  
**认证**: ✅ 需要 Token  
**权限**: User (只能更新自己的信息)  
**Content-Type**: `application/json`

**请求体** (`UserDTO`):

| 字段 | 类型 | 必填 | 说明 | 可更新 |
|------|------|------|------|--------|
| id | Long | ❌ | 用户ID（自动设置） | ❌ |
| nickname | String | ❌ | 昵称 | ✅ |
| email | String | ❌ | 邮箱 | ✅ |
| avatar | String | ❌ | 头像URL | ✅ |
| password | String | ❌ | 新密码 | ✅ |

> ⚠️ **安全限制**: 
> - `username` 不可修改
> - `status` 不可自行修改
> - `roles` 不可自行修改
> - 系统会自动将 `dto.id` 设置为当前登录用户ID

### 响应

**成功** (HTTP 200):

```json
{
  "code": 0,
  "message": "Success",
  "data": null
}
```

### 示例

```bash
curl -X PUT http://localhost:8080/api/users/me \\
  -H "Authorization: Bearer {token}" \\
  -H "Content-Type: application/json" \\
  -d '{
    "nickname": "New Nickname",
    "email": "newemail@example.com"
  }'
```

---

## GET /api/users/`{id}`

根据ID获取用户信息（管理员）

### 请求

**URL**: `GET /api/users/{id}`  
**认证**: ✅ 需要 Token  
**权限**: `ADMIN` 角色  

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户ID |

### 响应

**成功** (HTTP 200):

```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "id": 2,
    "username": "anotheruser",
    "email": "another@example.com",
    "nickname": "Another User",
    "status": 1,
    "roles": ["USER"],
    "createdAt": "2025-12-05T10:00:00"
  }
}
```

**失败场景**:

| Code | 消息 | 原因 |
|------|------|------|
| 403 | 无权限访问 | 非 ADMIN 角色 |
| 404 | 用户不存在 | ID 不存在 |

### 示例

```bash
curl -X GET http://localhost:8080/api/users/2 \
  -H "Authorization: Bearer {admin_token}"
```

---

## PUT /api/users/`{id}`

更新指定用户信息（管理员）

### 请求

**URL**: `PUT /api/users/{id}`  
**认证**: ✅ 需要 Token  
**权限**: `ADMIN` 角色  
**Content-Type**: `application/json`

**请求体** (`UserDTO`):

| 字段 | 可更新 | 说明 |
|------|--------|------|
| nickname | ✅ | 昵称 |
| email | ✅ | 邮箱 |
| status | ✅ | 状态（0=禁用, 1=启用）|
| password | ✅ | 新密码 |

### 响应

**成功** (HTTP 200):

```json
{
  "code": 0,
  "message": "Success",
  "data": null
}
```

### 示例

```bash
# 禁用用户
curl -X PUT http://localhost:8080/api/users/2 \
  -H "Authorization: Bearer {admin_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "status": 0
  }'

# 重置密码
curl -X PUT http://localhost:8080/api/users/2 \
  -H "Authorization: Bearer {admin_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "password": "NewPassword123!"
  }'
```

---

## DELETE /api/users/`{id}`

删除用户（管理员）

### 请求

**URL**: `DELETE /api/users/{id}`  
**认证**: ✅ 需要 Token  
**权限**: `ADMIN` 角色  

**路径参数**:

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户ID |

### 响应

**成功** (HTTP 200):

```json
{
  "code": 0,
  "message": "Success",
  "data": null
}
```

> ⚠️ **删除特性**: 
> - 删除操作是**幂等**的（删除不存在的用户也返回成功）
> - 实际行为取决于 BaseServiceImpl 实现（可能是软删除）

### 示例

```bash
curl -X DELETE http://localhost:8080/api/users/2 \
  -H "Authorization: Bearer {admin_token}"
```

---

## 权限说明

### User 权限

所有登录用户都可以：
- ✅ 查看自己的信息 (`/me`)
- ✅ 更新自己的资料 (`/me`)

### Admin 权限

管理员可以：
- ✅ 查看任意用户信息 (`/{id}`)
- ✅ 修改任意用户信息 (`/{id}`)
- ✅ 禁用/启用用户
- ✅ 删除用户

---

## 使用场景

### 场景1: 用户查看个人资料

```bash
# 1. 登录获取 Token
TOKEN=$(curl -X POST http://localhost:8080/auth/login ...)

# 2. 查看个人信息
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN"
```

### 场景2: 用户更新头像

```bash
# 1. 先上传图片到文件服务（假设返回 URL）
AVATAR_URL="https://s3.bitiful.net/blog-files/avatars/xxx.jpg"

# 2. 更新头像
curl -X PUT http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"avatar\": \"$AVATAR_URL\"}"
```

### 场景3: 管理员禁用用户

```bash
# 管理员禁用恶意用户
curl -X PUT http://localhost:8080/api/users/999 \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": 0}'
```

---

## 安全建议

1. **密码修改**: 
   - 前端应要求输入旧密码验证
   - 后端应发送邮件通知用户

2. **邮箱修改**: 
   - 应发送验证邮件到新邮箱
   - 验证通过后才生效

3. **删除用户**: 
   - 建议软删除（保留数据）
   - 删除前应检查关联数据（文章、评论等）

4. **Token 刷新**: 
   - 修改密码后应使旧 Token 失效
   - 需要用户重新登录

---

## 相关文档

- [认证 API](./auth) - 登录获取 Token
- [角色管理 API](./roles) - 角色权限管理
- [Base Framework](../development/base-framework) - updateByDto 安全机制  
- [Security 配置](../infrastructure/security/overview) - 权限控制原理
