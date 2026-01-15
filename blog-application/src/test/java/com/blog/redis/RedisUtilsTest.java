package com.blog.redis;

import com.blog.BlogApplication;
import com.blog.common.utils.RedisUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Redis 工具类集成测试套件 v1.0
 * <p>
 * 该测试套件旨在全面验证 {@link RedisUtils} 的功能完备性、健壮性和正确性。
 * 它继承了 {@code FrameworkIntegrationTest} 的设计哲学，通过模拟真实的应用场景，
 * 对 Redis 的四种核心数据结构（String, Hash, Set, List）的 CRUD 操作进行了地毯式测试。
 *
 * @see SpringBootTest 启动完整的 Spring Boot 应用上下文，确保 RedisTemplate 和 RedisUtils 被正确配置和注入。
 * @see ActiveProfiles 指定 "test" 环境，使用专为测试配置的 Redis 实例，实现环境隔离。
 * @see TestMethodOrder 通过 {@link MethodOrderer.OrderAnnotation} 控制测试方法的执行顺序，使测试流程更具逻辑性。
 * @see AfterEach 这是确保测试用例独立性的关键。每个测试方法执行后，此钩子会自动清理该方法在 Redis 中创建的所有测试键，
 * 防止测试间的状态污染。
 */
@SpringBootTest(classes = BlogApplication.class)
@ActiveProfiles("test")
@DisplayName("✅ Redis 工具类 (RedisUtils) 集成测试套件")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RedisUtilsTest {

    private static final Logger log = LoggerFactory.getLogger(RedisUtilsTest.class);
    // 用于记录本测试方法中创建的所有 Redis key，以便在测试结束后统一清理。
    private final List<String> keysToDelete = new ArrayList<>();
    @Resource
    private RedisUtils redisUtils;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 在每个测试方法执行【之后】运行。
     * <p>
     * 核心职责：清理当前测试方法在 Redis 中产生的所有数据。
     * 通过删除所有由 {@code keysToDelete} 列表追踪的键，保证了每个测试用例都在一个纯净、隔离的环境中执行，
     * 避免了测试之间的相互干扰，这是编写可靠集成测试的关键实践。
     */
    @AfterEach
    void cleanup() {
        if (!keysToDelete.isEmpty()) {
            log.info("▶️ 测试收尾: 准备清理 {} 个 Redis 键...", keysToDelete.size());
            Long deletedCount = redisUtils.delete(keysToDelete);
            log.info("✅ 清理完成: 成功删除了 {} 个键: {}", deletedCount, keysToDelete);
            keysToDelete.clear();
        }
    }

    /**
     * 辅助方法：注册一个键，以便在测试结束后自动删除。
     *
     * @param key 要追踪的 Redis 键
     * @return 传入的键，方便链式调用
     */
    private String track(String key) {
        keysToDelete.add(key);
        return key;
    }

    @Test
    @Order(1)
    @DisplayName("1. 环境加载与 Bean 注入测试")
    void contextLoadsAndRedisUtilsInjected() {
        log.info("▶️ 开始测试 (1/7): 环境加载与 RedisUtils Bean 注入...");
        assertNotNull(redisUtils, "RedisUtils 应该被成功注入，不能为 null");
        log.info("✅ 测试通过 (1/7): RedisUtils Bean 已正确注入。");
    }

    @Test
    @Order(2)
    @DisplayName("2. String (字符串) 类型操作全功能测试")
    void testStringOperations() throws InterruptedException {
        log.info("▶️ 开始测试 (2/7): String 类型操作...");
        final String key = track("test:string:user:1");
        final TestUser user = new TestUser(1L, "test-user");

        log.info("   - 步骤1: [SET/GET] 测试基本对象存取");
        redisUtils.set(key, user);
        TestUser retrievedUser = redisUtils.get(key, TestUser.class);
        assertEquals(user, retrievedUser, "存入和取出的对象应该完全相等");

        log.info("   - 步骤2: [Optional GET] 测试 Optional 安全获取");
        TestUser optionalUser = redisUtils.getOptional(key, TestUser.class)
                .orElseThrow(() -> new AssertionError("Optional不应为空"));
        assertEquals(user, optionalUser, "通过 Optional 获取的对象应该相等");
        assertTrue(redisUtils.getOptional("non-existent-key", TestUser.class).isEmpty(), "查询不存在的键应返回空的 Optional");

        log.info("   - 步骤3: [setEX] 测试带过期时间的存取");
        final String expiringKey = track("test:string:expiring");
        redisUtils.set(expiringKey, "will-expire", 1, TimeUnit.SECONDS);
        assertTrue(redisUtils.hasKey(expiringKey), "设置后，键应立即存在");
        Thread.sleep(1100); // 等待超过1秒以确保key已过期
        assertFalse(redisUtils.hasKey(expiringKey), "1秒后，键应该已过期并被删除");

        log.info("   - 步骤4: [INCR/DECR] 测试原子增减");
        final String counterKey = track("test:string:counter");
        redisUtils.set(counterKey, 0);
        assertEquals(1L, redisUtils.increment(counterKey), "调用 increment 后值应为 1");
        assertEquals(2L, redisUtils.increment(counterKey), "再次调用 increment 后值应为 2");
        assertEquals(1L, redisUtils.decrement(counterKey), "调用 decrement 后值应为 1");

        log.info("   - 步骤5: [SETNX] 测试 'setIfAbsent'");
        final String nxKey = track("test:string:nx");
        assertTrue(redisUtils.setIfAbsent(nxKey, "first-set", 10, TimeUnit.SECONDS), "第一次设置 (key不存在时) 应成功");
        assertFalse(redisUtils.setIfAbsent(nxKey, "second-set", 10, TimeUnit.SECONDS), "第二次设置 (key已存在时) 应失败");
        assertEquals("first-set", redisUtils.get(nxKey), "键的值应为第一次设置的值");

        log.info("✅ 测试通过 (2/7): String 类型所有操作均符合预期。");
    }

    @Test
    @Order(3)
    @DisplayName("3. Hash (哈希) 类型操作全功能测试")
    void testHashOperations() {
        log.info("▶️ 开始测试 (3/7): Hash 类型操作...");
        final String key = track("test:hash:user:profile:1");
        final TestUser user = new TestUser(1L, "test-user-hash");

        log.info("   - 步骤1: [hSet/hGET] 测试单个字段存取");
        redisUtils.hSet(key, "username", user.username());
        redisUtils.hSet(key, "id", user.id());
        assertEquals(user.username(), redisUtils.hGet(key, "username"), "HGET username 应返回正确的值");
        Number retrievedId = (Number) redisUtils.hGet(key, "id");
        assertNotNull(retrievedId, "从 Redis 获取的 ID 不应为 null");
        assertEquals(user.id(), retrievedId.longValue(), "hGET id 应返回正确的值");

        log.info("   - 步骤2: [hMSet/hGetAll] 测试多个字段批量存取");
        Map<String, Object> userMap = Map.of("username", "batch-user", "id", 2L, "status", "active");
        redisUtils.hSetAll(key, userMap);
        Map<Object, Object> retrievedMap = redisUtils.hGetAll(key);
        assertEquals(3, retrievedMap.size(), "hGetAll 应返回3个字段");
        assertEquals("batch-user", retrievedMap.get("username"), "批量设置的 username 应正确");

        log.info("   - 步骤3: [hDEL] 测试字段删除");
        Long deletedCount = redisUtils.hDel(key, "status", "non-existent-field");
        assertEquals(1L, deletedCount, "应成功删除1个存在的字段");
        assertNull(redisUtils.hGet(key, "status"), "被删除的字段 status 再次获取应为 null");

        log.info("✅ 测试通过 (3/7): Hash 类型所有操作均符合预期。");
    }

    @Test
    @Order(4)
    @DisplayName("4. Set (集合) 类型操作全功能测试")
    void testSetOperations() {
        log.info("▶️ 开始测试 (4/7): Set 类型操作...");
        final String key = track("test:set:user:roles:1");

        log.info("   - 步骤1: [sAdd] 测试添加元素");
        Long addedCount = redisUtils.sAdd(key, "admin", "editor", "viewer", "admin"); // 重复添加 "admin"
        assertEquals(3, addedCount, "sAdd 应该返回成功添加的新元素数量 (重复的 'admin' 不计数)");

        log.info("   - 步骤2: [sMembers] 测试获取所有元素");
        Set<Object> members = redisUtils.sMembers(key);
        assertAll("验证 Set 成员",
                () -> assertEquals(3, members.size(), "sMembers 返回的集合大小应为3"),
                () -> assertTrue(members.contains("admin"), "集合应包含 'admin'"),
                () -> assertTrue(members.contains("editor"), "集合应包含 'editor'")
        );

        log.info("✅ 测试通过 (4/7): Set 类型所有操作均符合预期。");
    }

    @Test
    @Order(5)
    @DisplayName("5. List (列表) 类型操作全功能测试")
    void testListOperations() {
        log.info("▶️ 开始测试 (5/7): List 类型操作...");
        final String key = track("test:list:user:actions");

        log.info("   - 步骤1: [lPush] 测试从左侧推入元素 (FILO 栈结构)");
        redisUtils.lPush(key, "login");
        redisUtils.lPush(key, "view-page");
        redisUtils.lPush(key, "logout"); // 列表当前为: [logout, view-page, login]

        log.info("   - 步骤2: [lRange] 测试范围获取");
        List<Object> actions = redisUtils.lRange(key, 0, -1);
        assertEquals(3, actions.size(), "LRANGE 应该返回所有3个元素");
        assertEquals("logout", actions.getFirst(), "列表的第一个元素应该是最后推入的 'logout'");

        log.info("   - 步骤3: [rPop] 测试从右侧弹出元素");
        Object lastAction = redisUtils.rPop(key); // 弹出 "login"
        assertEquals("login", lastAction, "rPop 应该弹出并返回最先推入的 'login'");
        assertEquals(2, redisUtils.lRange(key, 0, -1).size(), "弹出后，列表长度应为2");

        log.info("✅ 测试通过 (5/7): List 类型所有操作均符合预期。");
    }

    @Test
    @Order(6)
    @DisplayName("6. Common (通用) 操作与键管理测试")
    void testCommonKeyOperations() {
        log.info("▶️ 开始测试 (6/7): Common (通用) 操作...");
        final String key1 = track("test:common:key1");
        final String key2 = track("test:common:key2");

        redisUtils.set(key1, "value1");
        redisUtils.set(key2, "value2");

        log.info("   - 步骤1: [hasKey] 测试键是否存在");
        assertTrue(redisUtils.hasKey(key1), "存在的键 key1，hasKey 应返回 true");
        assertFalse(redisUtils.hasKey("non-existent-key"), "不存在的键，hasKey 应返回 false");

        log.info("   - 步骤2: [EXPIRE] 测试设置过期时间");
        assertTrue(redisUtils.expire(key1, 10, TimeUnit.SECONDS), "为存在的键设置过期应返回 true");

        log.info("   - 步骤3: [DELETE] 测试删除操作");
        assertTrue(redisUtils.delete(key1), "删除单个存在的键应返回 true");
        assertFalse(redisUtils.hasKey(key1), "删除后，key1 不应再存在");

        Long deletedCount = redisUtils.delete(List.of(key2, "non-existent-key"));
        assertEquals(1L, deletedCount, "批量删除应返回实际删除的键数量");
        assertFalse(redisUtils.hasKey(key2), "批量删除后，key2 不应再存在");

        log.info("✅ 测试通过 (6/7): Common (通用) 操作均符合预期。");
    }

    @Test
    @Order(7)
    @DisplayName("7. 健壮性与参数校验测试")
    void testRobustnessAndValidation() {
        log.info("▶️ 开始测试 (7/7): 健壮性与参数校验...");

        log.info("   - 步骤1: 测试 key 为 null 或空白字符串");
        assertAll("Key 参数校验",
                () -> assertThrows(IllegalArgumentException.class, () -> redisUtils.set(null, "value"), "key 为 null 时应抛出 IllegalArgumentException"),
                () -> assertThrows(IllegalArgumentException.class, () -> redisUtils.set("   ", "value"), "key 为空白字符串时应抛出 IllegalArgumentException"),
                () -> assertThrows(IllegalArgumentException.class, () -> redisUtils.get(null), "key 为 null 时应抛出 IllegalArgumentException")
        );

        log.info("   - 步骤2: 测试 value 为 null");
        assertThrows(NullPointerException.class, () -> redisUtils.set("some-key", null), "value 为 null 时应抛出 NullPointerException");

        log.info("   - 步骤3: 测试过期时间为负数");
        assertThrows(IllegalArgumentException.class, () -> redisUtils.expire("some-key", -1, TimeUnit.SECONDS), "过期时间为负数时应抛出 IllegalArgumentException");

        log.info("✅ 测试通过 (7/7): 参数校验机制工作正常，工具类健壮性得到验证。");
    }

    // 定义一个可序列化的内部记录(Record)，用于测试对象缓存。
    private record TestUser(Long id, String username) implements Serializable {
    }
}
