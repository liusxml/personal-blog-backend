package com.blog.config;

import com.blog.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 缓存管理控制器
 * <p>
 * 提供缓存管理接口，用于运维和监控
 * <p>
 * **注意**: 这些接口应该受权限保护，仅允许管理员访问
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/actuator/cache")
@RequiredArgsConstructor
@Tag(name = "缓存管理", description = "提供缓存查询、清除、预热等运维管理功能")
public class CacheManagementController {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheWarmup cacheWarmup;

    /**
     * 获取所有缓存名称和统计信息
     *
     * @return 缓存信息
     */
    @GetMapping
    @Operation(summary = "获取所有缓存", description = "查询当前应用中所有注册的缓存名称和统计信息")
    public Result<CacheInfoVO> getCaches() {
        Map<String, CacheDetailVO> cacheInfo = cacheManager.getCacheNames().stream()
                .collect(Collectors.toMap(
                        name -> name,
                        name -> {
                            Cache cache = cacheManager.getCache(name);
                            CacheDetailVO detail = new CacheDetailVO();
                            detail.setName(name);
                            if (cache != null) {
                                detail.setNativeCache(cache.getNativeCache().getClass().getSimpleName());
                            }
                            return detail;
                        }));

        CacheInfoVO result = new CacheInfoVO();
        result.setCacheNames(cacheManager.getCacheNames());
        result.setTotalCaches(cacheManager.getCacheNames().size());
        result.setCaches(cacheInfo);

        log.debug("查询缓存信息，共 {} 个缓存", result.getTotalCaches());
        return Result.success(result);
    }

    /**
     * 清除指定缓存
     *
     * @param cacheName 缓存名称
     * @return 操作结果
     */
    @DeleteMapping("/{cacheName}")
    @Operation(summary = "清除指定缓存", description = "根据缓存名称清除指定的缓存数据")
    public Result<CacheOperationVO> evictCache(
            @Parameter(description = "缓存名称", example = "user:roles") @PathVariable("cacheName") String cacheName) {

        Cache cache = cacheManager.getCache(cacheName);

        if (cache == null) {
            log.warn("缓存不存在: {}", cacheName);
            CacheOperationVO result = new CacheOperationVO();
            result.setSuccess(false);
            result.setMessage("缓存不存在: " + cacheName);
            return Result.success(result);
        }

        cache.clear();
        log.info("✅ 已清除缓存: {}", cacheName);

        CacheOperationVO result = new CacheOperationVO();
        result.setSuccess(true);
        result.setMessage("缓存已清除: " + cacheName);
        result.setCacheName(cacheName);

        return Result.success(result);
    }

    /**
     * 清除所有缓存
     *
     * @return 操作结果
     */
    @DeleteMapping
    @Operation(summary = "清除所有缓存", description = "清除应用中所有已注册的缓存数据")
    public Result<CacheOperationVO> evictAllCaches() {
        cacheWarmup.evictAllCaches();

        CacheOperationVO result = new CacheOperationVO();
        result.setSuccess(true);
        result.setMessage("所有缓存已清除");
        result.setClearedCaches(cacheManager.getCacheNames());

        log.info("✅ 已清除所有缓存，共 {} 个", result.getClearedCaches().size());
        return Result.success(result);
    }

    /**
     * 清除用户相关的所有缓存
     * <p>
     * 用于用户信息更新后主动失效缓存
     *
     * @param userId 用户ID
     * @return 操作结果
     */
    @DeleteMapping("/user/{userId}")
    @Operation(summary = "清除用户缓存", description = "清除指定用户的所有相关缓存（角色、详情等）")
    public Result<UserCacheOperationVO> evictUserCaches(
            @Parameter(description = "用户ID", example = "1") @PathVariable("userId") Long userId) {

        int clearedCount = 0;

        // 1. 清除用户角色缓存 (使用 CacheKeys 常量)
        Cache rolesCache = cacheManager.getCache("user:roles");
        if (rolesCache != null) {
            rolesCache.evict(userId);
            clearedCount++;
            log.info("清除用户角色缓存: userId={}", userId);
        }

        // 2. 清除用户详情缓存 (暂时保留，待后续统一管理)
        String userDetailKey = "user:detail:" + userId;
        Boolean deleted = redisTemplate.delete(userDetailKey);
        if (deleted) {
            clearedCount++;
            log.info("清除用户详情缓存: userId={}", userId);
        }

        // 3. 可扩展：清除其他用户相关缓存
        // - 用户资料缓存
        // - 用户权限缓存
        // - 用户设置缓存

        UserCacheOperationVO result = new UserCacheOperationVO();
        result.setSuccess(true);
        result.setMessage("用户缓存已清除");
        result.setUserId(userId);
        result.setClearedCount(clearedCount);

        log.info("✅ 已清除用户 {} 的缓存，共 {} 项", userId, clearedCount);
        return Result.success(result);
    }

    /**
     * 触发缓存预热
     *
     * @return 操作结果
     */
    @PostMapping("/warmup")
    @Operation(summary = "触发缓存预热", description = "手动触发缓存预热，加载常用数据到缓存中")
    public Result<CacheWarmupVO> warmupCache() {
        long startTime = System.currentTimeMillis();

        try {
            cacheWarmup.warmupCache();
            long duration = System.currentTimeMillis() - startTime;

            CacheWarmupVO result = new CacheWarmupVO();
            result.setSuccess(true);
            result.setMessage("缓存预热完成");
            result.setDuration(duration + "ms");

            log.info("✅ 缓存预热完成，耗时: {}ms", duration);
            return Result.success(result);
        } catch (Exception e) {
            log.error("缓存预热失败", e);

            CacheWarmupVO result = new CacheWarmupVO();
            result.setSuccess(false);
            result.setMessage("缓存预热失败: " + e.getMessage());

            return Result.success(result);
        }
    }

    /**
     * 获取 Redis 信息
     *
     * @return Redis 服务器信息
     */
    @GetMapping("/redis/info")
    @Operation(summary = "获取 Redis 信息", description = "查询 Redis 服务器的运行状态和详细信息")
    public Result<RedisInfoVO> getRedisInfo() {
        try {
            // 获取 Redis 连接工厂
            var connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                RedisInfoVO result = new RedisInfoVO();
                result.setSuccess(false);
                result.setConnected(false);
                result.setError("Redis ConnectionFactory 未配置");
                return Result.success(result);
            }

            // 获取 Redis 连接
            var connection = connectionFactory.getConnection();

            // 使用 serverCommands() 替代已弃用的 info()
            var serverCommands = connection.serverCommands();
            var info = serverCommands.info();

            // 检查 info 是否为 null
            String infoString = (info != null) ? info.toString() : "N/A";

            RedisInfoVO result = new RedisInfoVO();
            result.setSuccess(true);
            result.setConnected(true);
            result.setInfo(infoString);

            log.debug("获取 Redis 信息成功");
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取 Redis 信息失败", e);

            RedisInfoVO result = new RedisInfoVO();
            result.setSuccess(false);
            result.setConnected(false);
            result.setError(e.getMessage());

            return Result.success(result);
        }
    }

    // ========================== 内部 VO 类 ==========================

    /**
     * 缓存信息 VO
     */
    @lombok.Data
    public static class CacheInfoVO {
        private java.util.Collection<String> cacheNames;
        private int totalCaches;
        private Map<String, CacheDetailVO> caches;
    }

    /**
     * 缓存详情 VO
     */
    @lombok.Data
    public static class CacheDetailVO {
        private String name;
        private String nativeCache;
    }

    /**
     * 缓存操作结果 VO
     */
    @lombok.Data
    public static class CacheOperationVO {
        private boolean success;
        private String message;
        private String cacheName;
        private java.util.Collection<String> clearedCaches;
    }

    /**
     * 用户缓存操作结果 VO
     */
    @lombok.Data
    public static class UserCacheOperationVO {
        private boolean success;
        private String message;
        private Long userId;
        private int clearedCount;
    }

    /**
     * 缓存预热结果 VO
     */
    @lombok.Data
    public static class CacheWarmupVO {
        private boolean success;
        private String message;
        private String duration;
    }

    /**
     * Redis 信息 VO
     */
    @lombok.Data
    public static class RedisInfoVO {
        private boolean success;
        private boolean connected;
        private String info;
        private String error;
    }
}
