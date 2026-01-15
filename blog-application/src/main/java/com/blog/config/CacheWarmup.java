package com.blog.config;

import com.blog.system.domain.entity.RoleEntity;
import com.blog.system.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热组件
 * <p>
 * 在应用启动完成后自动预加载热点数据到 Redis，避免冷启动和缓存击穿。
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmup {

    private final RoleMapper roleMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheManager cacheManager;

    /**
     * 应用启动完成后执行缓存预热
     * <p>
     * 监听 {@link ApplicationReadyEvent} 事件，确保所有 Bean 已初始化完成
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmupCache() {
        long startTime = System.currentTimeMillis();
        log.info("🔥 开始缓存预热...");

        try {
            // 1. 预加载所有启用的角色
            warmupRoles();

            // 2. 可扩展：预加载其他热点数据

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ 缓存预热完成，耗时: {}ms", duration);
        } catch (Exception e) {
            log.error("❌ 缓存预热失败: {}", e.getMessage(), e);
            // 不抛出异常，避免影响应用启动
        }
    }

    /**
     * 预热角色数据
     */
    private void warmupRoles() {
        try {
            List<RoleEntity> roles = roleMapper.selectAllActive();

            if (CollectionUtils.isEmpty(roles)) {
                log.warn("⚠️  没有找到启用的角色数据，跳过角色缓存预热");
                return;
            }

            // 存入 Redis，过期时间 1 小时
            for (RoleEntity role : roles) {
                String key = "role:detail:" + role.getId();
                redisTemplate.opsForValue().set(key, role, 1, TimeUnit.HOURS);
            }

            log.info("✅ 角色缓存预热完成: 预加载 {} 个角色", roles.size());
        } catch (Exception e) {
            log.error("❌ 角色缓存预热失败: {}", e.getMessage());
        }
    }

    /**
     * 清除所有缓存（用于管理接口）
     * <p>
     * 注意：此方法会清空所有 Spring Cache，谨慎使用
     */
    public void evictAllCaches() {
        log.warn("⚠️  清除所有 Spring Cache 缓存");
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.info("清除缓存: {}", cacheName);
            }
        });
    }
}
