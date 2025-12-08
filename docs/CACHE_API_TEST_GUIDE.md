# 缓存管理 API 测试指南

本文档提供 `CacheManagementController` 所有接口的测试方法和示例。

## 📋 接口列表

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 获取所有缓存 | GET | `/actuator/cache` | 查看所有缓存信息 |
| 清除指定缓存 | DELETE | `/actuator/cache/{cacheName}` | 清除某个缓存 |
| 清除所有缓存 | DELETE | `/actuator/cache` | 清除全部缓存 |
| 清除用户缓存 | DELETE | `/actuator/cache/user/{userId}` | 清除某用户的缓存 |
| 触发缓存预热 | POST | `/actuator/cache/warmup` | 手动触发缓存预热 |
| 获取 Redis 信息 | GET | `/actuator/cache/redis/info` | 查看 Redis 服务器信息 |

---

## 🧪 测试方法

### 方式 1：使用 curl（推荐）

#### 1. 获取所有缓存信息

```bash
curl -X GET http://localhost:8080/actuator/cache \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**期望响应**：
```json
{
  "cacheNames": ["user:roles"],
  "totalCaches": 1,
  "caches": {
    "user:roles": {
      "name": "user:roles",
      "nativeCache": "DefaultRedisCacheWriter"
    }
  }
}
```

---

#### 2. 清除指定缓存

```bash
curl -X DELETE http://localhost:8080/actuator/cache/user:roles \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**期望响应**：
```json
{
  "success": true,
  "message": "缓存已清除: user:roles",
  "cacheName": "user:roles"
}
```

---

#### 3. 清除所有缓存

```bash
curl -X DELETE http://localhost:8080/actuator/cache \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**期望响应**：
```json
{
  "success": true,
  "message": "所有缓存已清除",
  "clearedCaches": ["user:roles"]
}
```

---

#### 4. 清除指定用户的缓存

```bash
# 清除用户 ID=1 的缓存
curl -X DELETE http://localhost:8080/actuator/cache/user/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**期望响应**：
```json
{
  "success": true,
  "message": "用户缓存已清除",
  "userId": 1,
  "clearedCount": 2
}
```

---

#### 5. 触发缓存预热

```bash
curl -X POST http://localhost:8080/actuator/cache/warmup \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**期望响应**：
```json
{
  "success": true,
  "message": "缓存预热完成",
  "duration": "256ms"
}
```

---

#### 6. 获取 Redis 服务器信息

```bash
curl -X GET http://localhost:8080/actuator/cache/redis/info \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**期望响应**：
```json
{
  "success": true,
  "connected": true,
  "info": "# Server\nredis_version:7.2.0\n..."
}
```

---

### 方式 2：使用 Postman / Insomnia

1. **导入到 Postman**

创建一个 Collection：`Cache Management API`

添加以下请求：

| 名称 | 方法 | URL |
|------|------|-----|
| Get All Caches | GET | `http://localhost:8080/actuator/cache` |
| Delete Cache | DELETE | `http://localhost:8080/actuator/cache/user:roles` |
| Delete All Caches | DELETE | `http://localhost:8080/actuator/cache` |
| Delete User Cache | DELETE | `http://localhost:8080/actuator/cache/user/1` |
| Warmup Cache | POST | `http://localhost:8080/actuator/cache/warmup` |
| Redis Info | GET | `http://localhost:8080/actuator/cache/redis/info` |

2. **设置 Headers**

```
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json
```

---

### 方式 3：使用 HTTPie（更友好的 CLI 工具）

```bash
# 安装 HTTPie
brew install httpie  # macOS
# 或 pip install httpie

# 获取缓存信息
http GET localhost:8080/actuator/cache \
  Authorization:"Bearer YOUR_TOKEN"

# 清除缓存
http DELETE localhost:8080/actuator/cache/user:roles \
  Authorization:"Bearer YOUR_TOKEN"

# 触发预热
http POST localhost:8080/actuator/cache/warmup \
  Authorization:"Bearer YOUR_TOKEN"
```

---

## 🔐 认证说明

这些接口需要认证。你需要：

### 方法 1：先登录获取 Token

```bash
# 1. 登录获取 token
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' \
  | jq -r '.data.token')

# 2. 使用 token 访问缓存 API
curl -X GET http://localhost:8080/actuator/cache \
  -H "Authorization: Bearer $TOKEN"
```

### 方法 2：临时禁用认证（仅开发环境）

如果想在开发时快速测试，可以临时修改 `SecurityConfig`，允许 `/actuator/cache/**` 路径：

```java
// 仅用于测试，不要提交到生产环境！
.requestMatchers("/actuator/cache/**").permitAll()
```

---

## 📊 验证流程示例

### 完整测试流程

```bash
# 1. 登录获取 token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' \
  | jq -r '.data.token')

echo "Token: $TOKEN"

# 2. 查看当前缓存状态
echo "\n=== 1. 查看所有缓存 ==="
curl -s -X GET http://localhost:8080/actuator/cache \
  -H "Authorization: Bearer $TOKEN" | jq

# 3. 触发缓存预热
echo "\n=== 2. 触发缓存预热 ==="
curl -s -X POST http://localhost:8080/actuator/cache/warmup \
  -H "Authorization: Bearer $TOKEN" | jq

# 4. 再次查看缓存（应该有数据了）
echo "\n=== 3. 预热后查看缓存 ==="
curl -s -X GET http://localhost:8080/actuator/cache \
  -H "Authorization: Bearer $TOKEN" | jq

# 5. 清除所有缓存
echo "\n=== 4. 清除所有缓存 ==="
curl -s -X DELETE http://localhost:8080/actuator/cache \
  -H "Authorization: Bearer $TOKEN" | jq

# 6. 验证缓存已清除
echo "\n=== 5. 验证缓存已清除 ==="
curl -s -X GET http://localhost:8080/actuator/cache \
  -H "Authorization: Bearer $TOKEN" | jq

# 7. 查看 Redis 信息
echo "\n=== 6. 查看 Redis 信息 ==="
curl -s -X GET http://localhost:8080/actuator/cache/redis/info \
  -H "Authorization: Bearer $TOKEN" | jq
```

保存为 `test_cache_api.sh`，然后运行：

```bash
chmod +x test_cache_api.sh
./test_cache_api.sh
```

---

## 🐛 常见问题

### 1. **403 Forbidden**

**原因**：没有提供有效的认证 Token

**解决**：先登录获取 Token，然后在请求头中添加：
```
Authorization: Bearer YOUR_TOKEN
```

### 2. **404 Not Found**

**原因**：路径错误或应用未启动

**检查**：
```bash
# 确认应用是否运行
curl http://localhost:8080/actuator/health
```

### 3. **缓存名称不存在**

**错误响应**：
```json
{
  "success": false,
  "message": "缓存不存在: xxx"
}
```

**解决**：使用 `GET /actuator/cache` 查看可用的缓存名称

---

## ✅ 期望的测试结果

### 正常流程

1. ✅ 获取缓存列表 → 看到 `user:roles`
2. ✅ 清除缓存 → 返回成功消息
3. ✅ 触发预热 → 返回预热完成和耗时
4. ✅ 查看 Redis 信息 → 返回连接状态和服务器信息

### Redis 中的变化

```bash
# 预热前
redis-cli KEYS "role:detail:*"
# (empty array)

# 预热后
redis-cli KEYS "role:detail:*"
# 1) "role:detail:1"
# 2) "role:detail:2"
# 3) "role:detail:3"

# 清除后
redis-cli KEYS "*"
# (empty array)
```

---

## 📝 测试检查清单

- [ ] 能成功获取缓存列表
- [ ] 能清除指定缓存
- [ ] 能清除所有缓存
- [ ] 能清除用户缓存
- [ ] 能触发缓存预热
- [ ] 能获取 Redis 信息
- [ ] 认证机制正常工作
- [ ] 错误处理正常（如删除不存在的缓存）

---

**测试完成后，记得验证 Redis 中的数据变化！** 🎉
