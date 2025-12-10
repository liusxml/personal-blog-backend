---
sidebar_position: 1
---

# Redis 使用指南

Redis 是 Personal Blog Backend 的核心基础设施之一，用于缓存、会话管理和分布式锁。本文档介绍 Redis 的使用规范和最佳实践。

## 🎯 使用场景

| 场景 | 数据结构 | 示例 |
|------|----------|------|
| **数据缓存** | String / Hash | 用户信息、文章详情 |
| **会话管理** | String | Spring Session |
| **分布式锁** | String (SET NX EX) | 防止重复提交 |
| **计数器** | String (INCR/DECR) | 文章浏览量、点赞数 |
| **排行榜** | Sorted Set | 热门文章、活跃用户 |
| **集合运算** | Set | 共同关注、标签交集 |

## 🔧 配置

### 基础配置

在 `application.yml` 中配置 Redis 连接：

```yaml title="blog-application/src/main/resources/application.yml"
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    password: ${REDIS_PASSWORD:}  # 从环境变量读取密码
    timeout: 3000ms
    
    # 连接池配置 (Lettuce)
    lettuce:
      pool:
        max-active: 8      # 最大连接数
        max-idle: 8        # 最大空闲连接
        min-idle: 2        # 最小空闲连接
        max-wait: 1000ms   # 最大等待时间
        
  # Spring Cache 配置
  cache:
    type: redis
    redis:
      time-to-live: 1800000  # 默认过期时间 30 分钟（毫秒）
      cache-null-values: false  # 不缓存 null 值
```

### 自定义配置类

```java title="blog-common/src/main/java/com/blog/common/config/RedisConfig.java"
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 使用 Jackson 序列化器
        Jackson2JsonRedisSerializer<Object> serializer = 
            new Jackson2JsonRedisSerializer<>(Object.class);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        serializer.setObjectMapper(mapper);
        
        // 设置序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))  // 默认过期时间
            .disableCachingNullValues()        // 不缓存 null
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new Jackson2JsonRedisSerializer<>(Object.class))
            );
        
        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .build();
    }
}
```

## 💾 Spring Cache 注解使用

### @Cacheable - 查询缓存

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends BaseServiceImpl<...> {
    
    /**
     * 缓存用户信息
     * - value: 缓存名称
     * - key: 缓存键，使用 SpEL 表达式
     * - unless: 条件，结果为 null 时不缓存
     */
    @Override
    @Cacheable(value = "user:detail", key = "#userId", unless = "#result == null")
    public UserDTO getUserById(Long userId) {
        log.info("从数据库查询用户: {}", userId);
        User user = userMapper.selectById(userId);
        return userConverter.toDto(user);
    }
    
    /**
     * 批量查询并缓存
     * - keyGenerator: 自定义键生成器
     */
    @Cacheable(value = "user:batch", keyGenerator = "userListKeyGenerator")
    public List<UserDTO> getUsersByIds(List<Long> userIds) {
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream()
            .map(userConverter::toDto)
            .collect(Collectors.toList());
    }
}
```

### @CacheEvict - 缓存失效

```java
@Service
public class UserServiceImpl extends BaseServiceImpl<...> {
    
    /**
     * 更新用户时失效缓存
     * - allEntries: 是否清除所有缓存
     * - beforeInvocation: 是否在方法执行前清除
     */
    @Override
    @CacheEvict(value = "user:detail", key = "#userDTO.id")
    public UserDTO updateUser(UserDTO userDTO) {
        log.info("更新用户并失效缓存: {}", userDTO.getId());
        return super.updateByDto(userDTO);
    }
    
    /**
     * 删除用户时失效多个缓存
     */
    @Override
    @CacheEvict(value = {"user:detail", "user:batch"}, key = "#userId")
    public boolean deleteUser(Long userId) {
        log.info("删除用户并失效缓存: {}", userId);
        return super.removeById(userId);
    }
    
    /**
     * 批量操作时清除所有缓存
     */
    @CacheEvict(value = "user:batch", allEntries = true)
    public void batchUpdateUsers(List<UserDTO> users) {
        log.info("批量更新用户，清除所有批量查询缓存");
        super.updateBatchByDto(users);
    }
}
```

### @CachePut - 更新缓存

```java
/**
 * 保存用户并更新缓存
 * - result: 方法返回值会更新到缓存
 * - 注意: CachePut 总是执行方法，不会从缓存读取
 */
@CachePut(value = "user:detail", key = "#result.id")
public UserDTO createUser(UserDTO userDTO) {
    log.info("创建用户并缓存: {}", userDTO.getUsername());
    return super.saveByDto(userDTO);
}
```

### @Caching - 组合注解

```java
/**
 * 复杂缓存操作 - 组合多个缓存注解
 */
@Caching(
    evict = {
        @CacheEvict(value = "user:list", allEntries = true),
        @CacheEvict(value = "user:count", allEntries = true)
    },
    put = {
        @CachePut(value = "user:detail", key = "#result.id")
    }
)
public UserDTO updateUserProfile(Long userId, UserDTO userDTO) {
    // 更新用户资料
    // - 失效列表缓存和统计缓存
    // - 更新详情缓存
    log.info("更新用户资料，组合缓存操作");
    return userService.updateByDto(userDTO);
}
```

## 🛠️ RedisUtils 工具类

对于 Spring Cache 无法覆盖的复杂操作，使用 `RedisUtils` 工具类：

```java
@Component
@RequiredArgsConstructor
public class RedisUtils {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // ========== String Operations ==========
    
    /**
     * 设置键值对
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }
    
    /**
     * 设置键值对并指定过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }
    
    /**
     * 获取值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    
    /**
     * 原子递增
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }
    
    /**
     * 原子递减
     */
    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }
    
    // ========== Hash Operations ==========
    
    /**
     * 设置 Hash 字段
     */
    public void hSet(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }
    
    /**
     * 获取 Hash 字段
     */
    public Object hGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }
    
    /**
     * 获取整个 Hash
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }
    
    // ========== Set Operations ==========
    
    /**
     * 添加到集合
     */
    public Long sAdd(String key, Object... values) {
        return redisTemplate.opsForSet().add(key, values);
    }
    
    /**
     * 获取集合所有元素
     */
    public Set<Object> sMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }
    
    // ========== Sorted Set Operations ==========
    
    /**
     * 添加到有序集合
     */
    public Boolean zAdd(String key, Object value, double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }
    
    /**
     * 获取排名（从高到低）
     */
    public Set<Object> zReverseRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }
    
    // ========== Common Operations ==========
    
    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }
    
    /**
     * 删除键
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }
    
    /**
     * 批量删除
     */
    public Long delete(Collection<String> keys) {
        return redisTemplate.delete(keys);
    }
    
    /**
     * 检查键是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }
}
```

## 📖 实战案例

### 案例 1: 文章浏览量统计

```java
@Service
@RequiredArgsConstructor
public class ArticleViewService {
    
    private final RedisUtils redisUtils;
    private final ArticleMapper articleMapper;
    
    private static final String VIEW_COUNT_KEY = "article:view:count:";
    
    /**
     * 增加文章浏览量
     */
    public void incrementViewCount(Long articleId) {
        String key = VIEW_COUNT_KEY + articleId;
        redisUtils.increment(key);
        
        // 设置过期时间（7天后同步到数据库）
        redisUtils.expire(key, 7, TimeUnit.DAYS);
    }
    
    /**
     * 获取文章浏览量
     */
    public Long getViewCount(Long articleId) {
        String key = VIEW_COUNT_KEY + articleId;
        Object count = redisUtils.get(key);
        
        if (count == null) {
            // 从数据库加载
            Article article = articleMapper.selectById(articleId);
            Long viewCount = article != null ? article.getViewCount() : 0L;
            redisUtils.set(key, viewCount, 7, TimeUnit.DAYS);
            return viewCount;
        }
        
        return Long.parseLong(count.toString());
    }
    
    /**
     * 定时任务：同步浏览量到数据库
     */
    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点执行
    public void syncViewCountToDatabase() {
        // 扫描所有浏览量键并同步
        // 实现略...
    }
}
```

### 案例 2: 热门文章排行榜

```java
@Service
@RequiredArgsConstructor
public class HotArticleService {
    
    private final RedisUtils redisUtils;
    private static final String HOT_ARTICLES_KEY = "article:hot:ranking";
    
    /**
     * 更新文章热度分数
     */
    public void updateHotScore(Long articleId, double score) {
        redisUtils.zAdd(HOT_ARTICLES_KEY, articleId, score);
    }
    
    /**
     * 获取热门文章TOP N
     */
    public List<Long> getHotArticles(int topN) {
        Set<Object> articles = redisUtils.zReverseRange(
            HOT_ARTICLES_KEY, 0, topN - 1
        );
        
        return articles.stream()
            .map(obj -> Long.parseLong(obj.toString()))
            .collect(Collectors.toList());
    }
}
```

## 📋 缓存键命名规范

遵循统一的命名规范，便于管理和调试：

```
格式: {模块}:{实体}:{操作}:{参数}

示例:
- user:detail:123           # 用户详情（ID=123）
- article:list:page:1       # 文章列表第1页
- comment:count:article:456 # 文章456的评论数
- cache:lock:order:789      # 订单789的分布式锁
```

:::tip 命名建议
- 使用冒号 `:` 分隔层级
- 不要使用空格或特殊字符
- 保持简洁但语义明确
- 使用小写字母和数字
:::

## ⚠️ 注意事项

### 1. 避免缓存穿透

```java
// ❌ 错误：缓存 null 值
@Cacheable(value = "user:detail", key = "#userId")
public UserDTO getUserById(Long userId) {
    return userMapper.selectById(userId);  // 可能返回 null
}

// ✅ 正确：使用 unless 条件
@Cacheable(value = "user:detail", key = "#userId", unless = "#result == null")
public UserDTO getUserById(Long userId) {
    return userMapper.selectById(userId);
}
```

### 2. 设置合理的过期时间

```java
// ❌ 错误：永不过期，可能导致内存泄漏
redisUtils.set("key", value);

// ✅ 正确：设置过期时间
redisUtils.set("key", value, 30, TimeUnit.MINUTES);
```

### 3. 缓存失效要及时

```java
@Service
public class UserServiceImpl {
    
    @CacheEvict(value = "user:detail", key = "#userId")
    public void updateUser(Long userId, UserDTO userDTO) {
        // 更新数据库
        userMapper.updateById(userConverter.toEntity(userDTO));
        // 缓存会自动失效
    }
}
```

## 📚 延伸阅读

<!-- 以下页面即将推出 -->
- **缓存策略详解** - 多级缓存、缓存一致性
- **分布式锁实现** - 基于 Redis 的分布式锁

---

**记住**：合理使用缓存可以显著提升系统性能，但也要注意缓存一致性和内存管理！
