---
sidebar_position: 2
---

# 缓存实现策略详解

## 📌 问题：为什么 CacheWarmup 不使用 RedisUtils？

你的项目中**同时使用了三种缓存方式**，每种都有其特定的使用场景。让我详细解释：

---

## 🎯 三种缓存实现方式

### 1. Spring Cache 注解（声明式缓存）⭐⭐⭐⭐⭐

**使用场景**: `UserServiceImpl.getUserRoleKeys()`

```java
@Cacheable(value = "user:roles", key = "#userId")
public List<String> getUserRoleKeys(Long userId) {
    log.debug("从数据库查询用户角色: userId={}", userId);
    List<SysRole> roles = userMapper.selectRolesByUserId(userId);
    return roles.stream()
            .map(role -> "ROLE_" + role.getRoleKey())
            .collect(Collectors.toList());
}
```

**优点**：
- ✅ **声明式编程** - 业务逻辑与缓存逻辑分离
- ✅ **自动管理** - Spring 自动处理缓存的读取、写入、失效
- ✅ **统一配置** - TTL、序列化等由 `RedisCacheManager` 统一管理
- ✅ **AOP 增强** - 无需侵入业务代码

**适用于**：
- ✅ 业务方法的返回值缓存
- ✅ 需要与业务逻辑绑定的缓存
- ✅ 需要统一管理的缓存策略

---

### 2. RedisUtils 工具类（命令式缓存）⭐⭐⭐⭐

**使用场景**: `RemoteUserServiceImpl.getUsersByIds()`

```java
public List<UserDTO> getUsersByIds(List<Long> ids) {
    // 1. 批量从 Redis 获取
    List<String> cacheKeys = ids.stream()
            .map(id -> "user:detail:" + id)
            .toList();
    
    List<Object> cachedUsers = redisUtils.mGet(cacheKeys);
    
    // 2. 找出缓存未命中的 ID
    List<Long> missingIds = new ArrayList<>();
    for (int i = 0; i < ids.size(); i++) {
        if (cachedUsers.get(i) == null) {
            missingIds.add(ids.get(i));
        }
    }
    
    // 3. 从数据库加载未命中的数据
    if (!missingIds.isEmpty()) {
        List<SysUser> dbUsers = userMapper.selectByIds(missingIds);
        // 写回 Redis...
    }
    
    return result;
}
```

**优点**：
- ✅ **精确控制** - 完全控制缓存的读写逻辑
- ✅ **批量操作** - 支持 MGET/MSET 高性能批量操作
- ✅ **灵活性** - Can handle complex caching scenarios
- ✅ **自定义TTL** - 每个操作可设置不同的过期时间

**适用于**：
- ✅ 需要批量操作的场景
- ✅ 复杂的缓存逻辑（如缓存未命中后的处理）
- ✅ 需要精确控制 TTL 的场景
- ✅ 跨服务的数据缓存

---

### 3. 直接使用 RedisTemplate（底层操作）⭐⭐⭐

**使用场景**: `CacheWarmup.warmupRoles()`

```java
private void warmupRoles() {
    try {
        List<SysRole> roles = roleMapper.selectAllActive();
        
        if (CollectionUtils.isEmpty(roles)) {
            log.warn("⚠️ 没有找到启用的角色数据，跳过角色缓存预热");
            return;
        }
        
        // 直接使用 RedisTemplate
        for (SysRole role : roles) {
            String key = "role:detail:" + role.getId();
            redisTemplate.opsForValue().set(key, role, 1, TimeUnit.HOURS);
        }
        
        log.info("✅ 角色缓存预热完成: 预加载 {} 个角色", roles.size());
    } catch (Exception e) {
        log.error("❌ 角色缓存预热失败: {}", e.getMessage());
    }
}
```

**为什么这里不使用 RedisUtils？**

1. **功能够用** - 简单的 `set` 操作，RedisTemplate 已经足够
2. **减少依赖** - `CacheWarmup` 是配置类，避免引入过多工具类
3. **独立性** - 预热逻辑与业务逻辑分离，使用最原始的 API
4. **性能考虑** - 避免额外的方法调用开销

**适用于**：
- ✅ 简单的 Redis 操作
- ✅ 配置类或工具类中的操作
- ✅ 不需要 RedisUtils 提供的高级功能

---

## 🏗️ 架构设计原则

### 选择缓存方式的决策树

```
开始
  │
  ├─ 是否是业务方法的返回值缓存？
  │   └─ 是 → 使用 Spring Cache 注解
  │       └─ @Cacheable / @CacheEvict / @CachePut
  │
  ├─ 是否需要批量操作 (MGET/MSET)？
  │   └─ 是 → 使用 RedisUtils
  │       └─ redisUtils.mGet() / mSet()
  │
  ├─ 是否有复杂的缓存逻辑？
  │   └─ 是 → 使用 RedisUtils
  │       └─ 完全控制缓存流程
  │
  └─ 简单的 Redis 操作
      └─ 使用 RedisTemplate 或 RedisUtils 都可以
```

---

## 📊 项目中的实际使用情况

### Spring Cache 注解使用

| 位置 | 方法 | 用途 |
|------|------|------|
| `UserServiceImpl` | `getUserRoleKeys()` | 缓存用户角色 |
| `UserServiceImpl` | `updateByDto()` | 更新时失效缓存 |

**配置位置**: `RedisConfig.cacheManager()`
- 统一 TTL: 30 分钟
- 统一序列化: Jackson
- 统一 Key 前缀: cacheName

---

### RedisUtils 使用

| 位置 | 方法 | 用途 |
|------|------|------|
| `RemoteUserServiceImpl` | `getUsersByIds()` | 批量获取用户（MGET） |
| `CacheManagementController` | 缓存管理 | 手动操作缓存 |

**特点**:
- 支持批量操作
- 自定义 TTL
- 完全控制缓存逻辑

---

### RedisTemplate 直接使用

| 位置 | 方法 | 用途 |
|------|------|------|
| `CacheWarmup` | `warmupRoles()` | 启动时预热角色缓存 |

**特点**:
- 简单直接
- 无需封装
- 配置类专用

---

## 💡 优化建议

### 建议 1: 保持现状（推荐）✅

**理由**:
- 架构清晰分层
- 各司其职
- 性能最优

### 建议 2: 统一使用 RedisUtils

如果你希望统一风格，可以这样修改 `CacheWarmup`:

```java
private void warmupRoles() {
    try {
        List<SysRole> roles = roleMapper.selectAllActive();
        
        if (CollectionUtils.isEmpty(roles)) {
            log.warn("⚠️ 没有找到启用的角色数据，跳过角色缓存预热");
            return;
        }
        
        // 使用 RedisUtils替代直接使用 RedisTemplate
        for (SysRole role : roles) {
            String key = "role:detail:" + role.getId();
            redisUtils.set(key, role, 1, TimeUnit.HOURS);
            // 或者使用 TTL 随机化防止雪崩
            // redisUtils.setWithRandomTTL(key, role, 1, TimeUnit.HOURS, 10);
        }
        
        log.info("✅ 角色缓存预热完成: 预加载 {} 个角色", roles.size());
    } catch (Exception e) {
        log.error("❌ 角色缓存预热失败: {}", e.getMessage());
    }
}
```

**优点**:
- ✅ 代码风格统一
- ✅ 可以使用 TTL 随机化防止缓存雪崩
- ✅ 日志记录更完整（RedisUtils 内部有 debug 日志）

**缺点**:
- ❌ 增加了一层方法调用
- ❌ 增加了 RedisUtils 依赖注入

### 建议 3: 使用批量操作优化

如果角色数据较多，可以使用批量写入：

```java
private void warmupRoles() {
    try {
        List<SysRole> roles = roleMapper.selectAllActive();
        
        if (CollectionUtils.isEmpty(roles)) {
            log.warn("⚠️ 没有找到启用的角色数据，跳过角色缓存预热");
            return;
        }
        
        // 使用 MSET 批量写入
        Map<String, Object> cacheData = roles.stream()
                .collect(Collectors.toMap(
                        role -> "role:detail:" + role.getId(),
                        role -> role
                ));
        
        redisUtils.mSet(cacheData);
        
        // 批量设置过期时间（需要额外实现）
        // 或者保持使用单个 set 以支持 TTL
        
        log.info("✅ 角色缓存预热完成: 预加载 {} 个角色", roles.size());
    } catch (Exception e) {
        log.error("❌ 角色缓存预热失败: {}", e.getMessage());
    }
}
```

**注意**: `MSET` 不支持设置过期时间，需要后续批量 `EXPIRE`。

---

## ✅ 最佳实践总结

### 什么时候用 Spring Cache？

- ✅ 业务方法的返回值缓存
- ✅ 需要声明式编程
- ✅ 缓存逻辑简单
- ✅ 需要统一管理 TTL

### 什么时候用 RedisUtils？

- ✅ 需要批量操作（MGET/MSET）
- ✅ 复杂的缓存逻辑
- ✅ 需要自定义 TTL
- ✅ 需要防雪崩（TTL 随机化）

### 什么时候直接用 RedisTemplate？

- ✅ 简单的 Redis 操作
- ✅ 配置类或工具类
- ✅ 不需要 RedisUtils 的高级功能
- ✅ 减少依赖注入

---

## 🎯 你的项目现状

**非常好！** ✅

你的项目已经很好地实践了"**合适的工具用在合适的场景**"的原则：

1. **Spring Cache** - 用于 `UserServiceImpl.getUserRoleKeys()`
2. **RedisUtils** - 用于 `RemoteUserServiceImpl.getUsersByIds()` 批量操作
3. **RedisTemplate** - 用于 `CacheWarmup` 简单预热

这是一个**清晰、高效、易维护**的架构设计！

---

**建议**: 如果你想更统一，可以将 `CacheWarmup` 改为使用 `RedisUtils`，但保持现状也完全没问题。
