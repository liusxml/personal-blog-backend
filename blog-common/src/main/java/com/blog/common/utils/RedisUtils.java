package com.blog.common.utils;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 通用工具类
 * <p>
 * 基于 Spring {@link Redis Template} 封装，提供对 String、Hash、Set、List 等数据结构的便捷操作。
 * <p>
 * <b>核心特性：</b>
 * <ul>
 * <li><b>参数校验</b>：使用 Guava Preconditions 和 Commons Lang3 保证输入合法性</li>
 * <li><b>日志记录</b>：SLF4J 参数化日志，便于问题排查</li>
 * <li><b>类型安全</b>：{@link Optional} 包装返回值，避免空指针异常</li>
 * <li><b>批量操作</b>：支持 MGET/MSET 批量操作，提升性能</li>
 * <li><b>防雪崩</b>：setWithRandomTTL 方法支持随机 TTL，避免缓存雪崩</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 *
 * <pre>{@code
 * // String 操作
 * redisUtils.set("key", "value", 30, TimeUnit.MINUTES);
 * Optional<Object> value = redisUtils.get("key");
 *
 * // 批量操作
 * List<Object> values = redisUtils.mGet(Arrays.asList("key1", "key2"));
 *
 * // 防雪崩
 * red isUtils.setWithRandomTTL("key", value, 30, TimeUnit.MINUTES, 10);
 * }</pre>
 *
 * @author liusxml
 * @version 4.0
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class RedisUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    // ============================= Private Helpers =============================

    /**
     * 内部方法，校验 key 是否为空白。
     *
     * @param key 待校验的 key
     */
    private void checkKey(final String key) {
        Preconditions.checkArgument(StringUtils.isNotBlank(key), "Redis 的 key 不能为空白字符串。");
    }

    // ============================= Common (通用操作) =============================

    /**
     * 判断 key 是否存在 (EXISTS)
     *
     * @param key 键, 非空
     * @return {@code true} 如果存在, 否则 {@code false}
     */
    public boolean hasKey(final String key) {
        checkKey(key);
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置 key 的过期时间 (EXPIRE)
     *
     * @param key     键, 非空
     * @param timeout 过期时间, 必须大于 0
     * @param unit    时间单位, 非空
     * @return {@code true} 设置成功, {@code false} 键不存在或设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit) {
        checkKey(key);
        Preconditions.checkNotNull(unit, "时间单位不能为空。");
        Preconditions.checkArgument(timeout > 0, "过期时间必须大于0。");
        log.debug("为键 '{}' 设置过期时间: {} {}", key, timeout, unit.toString().toLowerCase());
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 删除一个或多个 key (DEL)
     *
     * @param keys 要删除的键集合, 非空
     * @return 成功删除的 key 的数量
     */
    public Long delete(final Collection<String> keys) {
        Preconditions.checkNotNull(keys, "要删除的 key 集合不能为空。");
        if (keys.isEmpty()) {
            return 0L;
        }
        log.debug("准备批量删除 Redis 键: {}", keys);
        return redisTemplate.delete(keys);
    }

    /**
     * 删除单个 key (DEL)
     *
     * @param key 要删除的键, 非空
     * @return {@code true} 如果删除成功, 否则 {@code false}
     */
    public boolean delete(final String key) {
        checkKey(key);
        log.debug("准备删除 Redis 键: '{}'", key);
        return redisTemplate.delete(key);
    }

    // ============================ String (字符串) =============================

    /**
     * 缓存放入 (SET)
     *
     * @param key   键, 非空
     * @param value 值, 非空
     */
    public void set(final String key, final Object value) {
        checkKey(key);
        Preconditions.checkNotNull(value, "缓存的值不能为空。");
        redisTemplate.opsForValue().set(key, value);
        log.debug("Redis [SET] - 键: '{}', 值: '{}'", key, value);
    }

    /**
     * 缓存放入并设置过期时间 (setEX)
     *
     * @param key     键, 非空
     * @param value   值, 非空
     * @param timeout 过期时间, 必须大于 0
     * @param unit    时间单位, 非空
     */
    public void set(final String key, final Object value, final long timeout, final TimeUnit unit) {
        checkKey(key);
        Preconditions.checkNotNull(value, "缓存的值不能为空。");
        Preconditions.checkNotNull(unit, "时间单位不能为空。");
        Preconditions.checkArgument(timeout > 0, "过期时间必须大于0。");
        redisTemplate.opsForValue().set(key, value, timeout, unit);
        log.debug("Redis [setEX] - 键: '{}', 值: '{}', 过期时间: {} {}", key, value, timeout, unit.toString().toLowerCase());
    }

    /**
     * 只有在 key 不存在时才设置 (setNX)
     *
     * @param key     键, 非空
     * @param value   值, 非空
     * @param timeout 过期时间, 必须大于 0
     * @param unit    时间单位, 非空
     * @return {@code true} 如果设置成功, {@code false} 如果 key 已存在
     */
    public boolean setIfAbsent(final String key, final Object value, final long timeout, final TimeUnit unit) {
        checkKey(key);
        Preconditions.checkNotNull(value, "缓存的值不能为空。");
        Preconditions.checkNotNull(unit, "时间单位不能为空。");
        Preconditions.checkArgument(timeout > 0, "过期时间必须大于0。");
        boolean success = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit));
        if (success) {
            log.debug("Redis [setNX] 成功 - 键: '{}', 值: '{}', 过期时间: {} {}", key, value, timeout,
                    unit.toString().toLowerCase());
        }
        return success;
    }

    /**
     * 缓存获取 (GET)
     *
     * @param key 键, 非空
     * @return 缓存的值, 可能为 {@code null}
     */
    public Object get(final String key) {
        checkKey(key);
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 缓存获取并转换为指定类型
     *
     * @param key   键, 非空
     * @param clazz 目标类型的 Class 对象, 非空
     * @param <T>   目标类型
     * @return 转换后的对象, 如果 key 不存在或类型不匹配则返回 {@code null}
     */
    public <T> T get(final String key, final Class<T> clazz) {
        checkKey(key);
        Preconditions.checkNotNull(clazz, "目标 Class 类型不能为空。");
        Object value = redisTemplate.opsForValue().get(key);
        return value == null ? null : clazz.cast(value);
    }

    /**
     * 以 {@link Optional} 形式安全地获取缓存并转换类型
     *
     * @param key   键, 非空
     * @param clazz 目标类型的 Class 对象, 非空
     * @param <T>   目标类型
     * @return 包含值的 {@link Optional}, 或一个空的 {@link Optional}
     */
    public <T> Optional<T> getOptional(final String key, final Class<T> clazz) {
        return Optional.ofNullable(get(key, clazz));
    }

    /**
     * 原子递增 (INCR)
     *
     * @param key 键, 非空
     * @return 递增后的值
     */
    public Long increment(final String key) {
        checkKey(key);
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 原子递减 (DECR)
     *
     * @param key 键, 非空
     * @return 递减后的值
     */
    public Long decrement(final String key) {
        checkKey(key);
        return redisTemplate.opsForValue().decrement(key);
    }

    // ================================ Hash (哈希) =================================

    /**
     * 获取哈希表中指定字段的值 (hGET)
     *
     * @param key     键, 非空
     * @param hashKey 项, 非空
     * @return 值, 可能为 {@code null}
     */
    public Object hGet(final String key, final String hashKey) {
        checkKey(key);
        Preconditions.checkArgument(StringUtils.isNotBlank(hashKey), "哈希的 field 不能为空白字符串。");
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * 获取哈希表中的所有键值对 (hGetAll)
     *
     * @param key 键, 非空
     * @return 包含所有键值对的 Map
     */
    public Map<Object, Object> hGetAll(final String key) {
        checkKey(key);
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 向哈希表中放入数据 (hSetAll)
     *
     * @param key 键, 非空
     * @param map 对应的多个键值, 非空
     */
    public void hSetAll(final String key, final Map<String, Object> map) {
        checkKey(key);
        Preconditions.checkNotNull(map, "存入哈希的 Map 不能为空。");
        redisTemplate.opsForHash().putAll(key, map);
        log.debug("Redis [hSetAll] - 键: '{}', 存入 {} 个键值对", key, map.size());
    }

    /**
     * 向哈希表中放入单个数据 (hSet)
     *
     * @param key     键, 非空
     * @param hashKey 项, 非空
     * @param value   值, 非空
     */
    public void hSet(final String key, final String hashKey, final Object value) {
        checkKey(key);
        Preconditions.checkArgument(StringUtils.isNotBlank(hashKey), "哈希的 field 不能为空白字符串。");
        Preconditions.checkNotNull(value, "存入哈希的值不能为空。");
        redisTemplate.opsForHash().put(key, hashKey, value);
        log.debug("Redis [hSet] - 键: '{}', Field: '{}', 值: '{}'", key, hashKey, value);
    }

    /**
     * 删除哈希表中的一个或多个字段 (hDel)
     *
     * @param key      键, 非空
     * @param hashKeys 项, 可以是多个, 非空
     * @return 删除成功的字段数量
     */
    public Long hDel(final String key, final Object... hashKeys) {
        checkKey(key);
        Preconditions.checkNotNull(hashKeys, "要删除的哈希 field 不能为空。");
        log.debug("Redis [hDel] - 键: '{}', 准备删除 Fields: {}", key, hashKeys);
        return redisTemplate.opsForHash().delete(key, hashKeys);
    }

    // ============================ Set (集合) =============================

    /**
     * 将数据放入 set 缓存 (sAdd)
     *
     * @param key    键, 非空
     * @param values 值, 可以是多个, 非空
     * @return 成功存入的数量
     */
    public Long sAdd(final String key, final Object... values) {
        checkKey(key);
        Preconditions.checkNotNull(values, "存入 Set 的值不能为空。");
        log.debug("Redis [sAdd] - 键: '{}', 存入值: {}", key, values);
        return redisTemplate.opsForSet().add(key, values);
    }

    /**
     * 获取 set 缓存的所有成员 (sMembers)
     *
     * @param key 键, 非空
     * @return Set 集合
     */
    public Set<Object> sMembers(final String key) {
        checkKey(key);
        return redisTemplate.opsForSet().members(key);
    }

    // ============================ List (列表) =============================

    /**
     * 从列表左侧（头部）插入数据 (lPush)
     *
     * @param key   键, 非空
     * @param value 值, 非空
     * @return 列表的长度
     */
    public Long lPush(final String key, final Object value) {
        checkKey(key);
        Preconditions.checkNotNull(value, "存入 List 的值不能为空。");
        log.debug("Redis [lPush] - 键: '{}', 从左侧推入值: '{}'", key, value);
        return redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * 从列表右侧（尾部）弹出数据 (rPop)
     *
     * @param key 键, 非空
     * @return 弹出的元素, 列表为空时返回 {@code null}
     */
    public Object rPop(final String key) {
        checkKey(key);
        Object value = redisTemplate.opsForList().rightPop(key);
        log.debug("Redis [rPop] - 键: '{}', 从右侧弹出值: '{}'", key, value);
        return value;
    }

    /**
     * 获取列表指定范围内的元素 (lRange)
     *
     * @param key   键, 非空
     * @param start 开始索引, 0 表示第一个元素
     * @param end   结束索引, -1 表示最后一个元素
     * @return 元素列表
     */
    public List<Object> lRange(final String key, final long start, final long end) {
        checkKey(key);
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 获取列表长度 (LLEN)
     *
     * @param key 键, 非空
     * @return 列表长度
     */
    public Long lSize(final String key) {
        checkKey(key);
        return redisTemplate.opsForList().size(key);
    }

    // ============================= Batch
    // Operations（批量操作）=============================

    /**
     * 批量获取（MGET）
     * <p>
     * 性能优化：将 N 次网络请求合并为 1 次
     *
     * @param keys 键集合，非空
     * @return 值列表，顺序与 keys 一致，不存在的键对应 null
     */
    public List<Object> mGet(final Collection<String> keys) {
        Preconditions.checkNotNull(keys, "要获取的 key 集合不能为空。");
        if (keys.isEmpty()) {
            return List.of();
        }
        // 限制批量大小，防止慢查询
        Preconditions.checkArgument(keys.size() <= 100, "批量获取的 key 数量不能超过 100。");

        log.debug("Redis [MGET] - 批量获取 {} 个键", keys.size());
        return redisTemplate.opsForValue().multiGet(keys);
    }

    /**
     * 批量设置（MSET）
     * <p>
     * 性能优化：将 N 次网络请求合并为 1 次
     *
     * @param map 键值对 Map，非空
     */
    public void mSet(final Map<String, Object> map) {
        Preconditions.checkNotNull(map, "要设置的键值对 Map 不能为空。");
        if (map.isEmpty()) {
            return;
        }
        // 限制批量大小
        Preconditions.checkArgument(map.size() <= 100, "批量设置的键值对数量不能超过 100。");

        log.debug("Redis [MSET] - 批量设置 {} 个键值对", map.size());
        redisTemplate.opsForValue().multiSet(map);
    }

    /**
     * 设置值并添加随机 TTL（防止缓存雪崩）
     * <p>
     * 在基础过期时间上添加随机偏移，避免大量缓存同时过期
     *
     * @param key           键，非空
     * @param value         值，非空
     * @param baseTimeout   基础过期时间
     * @param unit          时间单位
     * @param randomPercent 随机百分比（0-100），例如 10 表示 ±10%
     */
    public void setWithRandomTTL(final String key, final Object value,
                                 final long baseTimeout, final TimeUnit unit,
                                 final int randomPercent) {
        checkKey(key);
        Preconditions.checkNotNull(value, "缓存的值不能为空。");
        Preconditions.checkNotNull(unit, "时间单位不能为空。");
        Preconditions.checkArgument(baseTimeout > 0, "过期时间必须大于0。");
        Preconditions.checkArgument(randomPercent >= 0 && randomPercent <= 100,
                "随机百分比必须在 0-100 之间。");

        // 计算随机偏移
        long baseSeconds = unit.toSeconds(baseTimeout);
        long randomOffset = (long) (baseSeconds * randomPercent / 100.0 *
                (Math.random() * 2 - 1)); // -randomPercent% ~ +randomPercent%
        long finalTimeout = baseSeconds + randomOffset;

        redisTemplate.opsForValue().set(key, value, finalTimeout, TimeUnit.SECONDS);
        log.debug("Redis [setWithRandomTTL] - 键: '{}', 基础TTL: {}s, 随机偏移: {}s, 最终TTL: {}s",
                key, baseSeconds, randomOffset, finalTimeout);
    }
}
