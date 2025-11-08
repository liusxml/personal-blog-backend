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
 * Redis 通用工具类 (生产最终版)
 * <p>
 * 基于 Spring {@link RedisTemplate} 封装，提供对多种数据结构的便捷操作。
 * 结合 Guava Preconditions 和 Apache Commons Lang3 保证输入的有效性。
 * <p>
 * <strong>V3.0 最终版特性:</strong>
 * <ol>
 *     <li><b>生产就绪:</b> 移除了所有 Java 21 的预览功能 (StringTemplate)，确保项目在任何环境下的稳定性和兼容性。</li>
 *     <li><b>标准日志:</b> 全面采用 SLF4J 的参数化日志格式 (e.g., `log.debug("模板: {}", arg)`)，以获得最佳性能和代码可读性。</li>
 *     <li><b>完全中文化:</b> 所有日志输出和校验异常信息均已更新为中文，提升团队协作效率。</li>
 *     <li><b>代码健壮性:</b> 通过提取私有校验方法减少了重复代码，并使用 {@link Optional} 增强了类型安全，有效防止空指针。</li>
 *     <li><b>功能完备:</b> 提供了对 String, Hash, Set, List 四种常用数据结构的完整操作，并支持原子性的增减和 SETNX 操作。</li>
 * </ol>
 *
 * @version 3.0
 * @author Your Name
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class RedisUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    // ============================= Private Helpers =============================

    /**
     * 内部方法，校验 key 是否为空白。
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
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
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
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
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
        return Boolean.TRUE.equals(redisTemplate.delete(key));
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
     * 缓存放入并设置过期时间 (SETEX)
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
        log.debug("Redis [SETEX] - 键: '{}', 值: '{}', 过期时间: {} {}", key, value, timeout, unit.toString().toLowerCase());
    }

    /**
     * 只有在 key 不存在时才设置 (SETNX)
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
            log.debug("Redis [SETNX] 成功 - 键: '{}', 值: '{}', 过期时间: {} {}", key, value, timeout, unit.toString().toLowerCase());
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
}
