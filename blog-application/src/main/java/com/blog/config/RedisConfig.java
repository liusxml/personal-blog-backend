package com.blog.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * Redis 配置类
 * <p>
 * 配置 RedisTemplate、RedisCacheManager 和监控指标，确保与
 * {@link com.blog.common.utils.RedisUtils} 协同工作。
 * <p>
 * <b>核心配置：</b>
 * <ul>
 * <li><b>RedisTemplate</b>：使用 Jackson 序列化，支持 Java 8 时间类型</li>
 * <li><b>RedisCacheManager</b>：支持 Spring Cache 注解，默认 TTL 30 分钟</li>
 * <li><b>Micrometer Metrics</b>：暴露 Lettuce 连接状态到 /actuator/metrics</li>
 * </ul>
 * <p>
 * <b>序列化策略：</b>
 * <ul>
 * <li>Key：{@link StringRedisSerializer} - 可读的字符串格式</li>
 * <li>Value：{@link GenericJackson2JsonRedisSerializer} - JSON 格式 + 类型信息</li>
 * </ul>
 * <p>
 * <b>防穿透机制：</b> 禁止缓存 null 值
 *
 * @author liusxml
 * @version 2.0
 * @see com.blog.common.utils.RedisUtils
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * 创建 ObjectMapper 用于 Redis JSON 序列化
     * <p>
     * <b>关键配置：</b>
     * <ul>
     * <li>保留类型信息（@class 属性）：避免反序列化类型丢失</li>
     * <li>支持 Java 8 时间：LocalDateTime、LocalDate 等使用 ISO-8601 格式</li>
     * <li>禁用时间戳：2025-12-07T23:00:00 而非 1733584800000</li>
     * </ul>
     *
     * @return 配置好的 ObjectMapper 实例
     */
    private ObjectMapper createRedisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 设置所有字段可见
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 启用默认类型信息，防止反序列化时类型丢失
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        // 注册 Java 8 时间模块
        objectMapper.registerModule(new JavaTimeModule());

        // 禁用时间戳格式，使用 ISO-8601 字符串格式
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }

    /**
     * 配置 RedisTemplate Bean
     * <p>
     * 作为 {@link com.blog.common.utils.RedisUtils} 的底层操作模板。
     * <p>
     * <b>序列化策略：</b> Key 使用 String，Value 使用 Jackson JSON
     *
     * @param connectionFactory Redis 连接工厂（由 Spring Boot 自动配置）
     * @return RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 序列化器（String）
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        // Value 序列化器（Jackson）
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer(
                createRedisObjectMapper());
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置 RedisCacheManager Bean
     * <p>
     * 支持 Spring Cache 注解（@Cacheable、@CacheEvict、@CachePut）。
     * <p>
     * <b>默认配置：</b>
     * <ul>
     * <li>TTL：30 分钟</li>
     * <li>Key 序列化：String</li>
     * <li>Value 序列化：Jackson JSON</li>
     * <li>Null 值：禁止缓存（防穿透）</li>
     * </ul>
     *
     * @param connectionFactory Redis 连接工厂
     * @return RedisCacheManager 实例
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 序列化配置
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer(
                createRedisObjectMapper());

        // 缓存配置
        RedisCacheConfiguration config = RedisCacheConfiguration
                .defaultCacheConfig()
                // 设置默认过期时间为 30 分钟
                .entryTtl(Duration.ofMinutes(30))
                // Key 序列化器
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                // Value 序列化器
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jackson2JsonRedisSerializer))
                // 禁止缓存 null 值，避免缓存穿透
                .disableCachingNullValues();

        // 为特定缓存配置不同的 TTL（可选）
        Map<String, RedisCacheConfiguration> cacheConfigurations = new java.util.HashMap<>();

        // user:roles 缓存：30 分钟
        cacheConfigurations.put("user:roles", config);

        // 如果需要为其他缓存配置不同的 TTL，可以在这里添加

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .initialCacheNames(Set.of("user:roles")) // 预先注册缓存名称
                .withInitialCacheConfigurations(cacheConfigurations) // 配置每个缓存的 TTL
                .build();
    }

    /**
     * 配置 Lettuce 监控指标
     * <p>
     * 暴露 Redis 客户端监控指标到 Micrometer，可通过
     * {@code /actuator/metrics/redis.lettuce.factory.active} 查看。
     * <p>
     * <b>指标说明：</b> redis.lettuce.factory.active = 1.0 表示连接工厂正常
     *
     * @param connectionFactory Redis 连接工厂
     * @return MeterBinder 指标绑定器
     */
    @Bean
    public MeterBinder redisMetrics(
            RedisConnectionFactory connectionFactory) {
        return (registry) -> {
            // 注册 Lettuce 连接状态指标
            if (connectionFactory instanceof LettuceConnectionFactory) {
                registry.gauge("redis.lettuce.factory.active",
                        connectionFactory, f -> 1.0); // 连接工厂存在即为活跃

                log.info("✅ Redis Lettuce 监控指标已启用");
            }
        };
    }
}
