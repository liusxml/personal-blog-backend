package com.blog.frameworktest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.filter.LevelFilter;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.blog.BlogApplication;
import com.blog.common.enums.SystemErrorCode;
import com.blog.config.ddl.DdlInitializer;
import com.blog.frameworktest.dto.TestDTO;
import com.blog.frameworktest.dto.ValidationTestDTO;
import com.blog.frameworktest.service.ITestService;
import com.blog.frameworktest.vo.TestVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 核心框架集成测试套件 v1.0
 * <p>
 * 这是一份高度综合的集成测试类，其核心目标是验证整个Spring Boot应用的核心组件能否精确、无缝地协同工作。
 * 它如同一份项目的“健康诊断报告”，覆盖了从应用启动、Bean注入、核心配置加载（日志、MyBatis-Plus）、
 * 数据库交互到Web层模拟请求的完整生命周期。通过此测试，可以极大地增强对底层框架稳定性和正确性的信心。
 *
 * @see SpringBootTest 启动一个完整的、真实的Spring Boot应用上下文，这是进行深度集成测试的基石。
 * @see ActiveProfiles 强制指定使用 "test" 配置文件 (application-test.yml)。这实现了开发、测试、生产环境的完全隔离，是专业测试实践的关键。
 * @see AutoConfigureMockMvc 自动配置 {@link MockMvc} 实例，它是模拟HTTP请求、测试Controller层的核心工具，无需手动搭建。
 * @see Transactional 这是一个至关重要的注解。它能确保每个测试方法都在一个独立的数据库事务中运行。测试方法执行完毕后，该事务会自动回滚。
 *                  这样做的好处是，无论测试中对数据库做了任何增删改操作，都不会污染数据库，保证了每个测试用例的独立性和可重复性。
 * @see TestMethodOrder 通过 {@link MethodOrderer.OrderAnnotation} 控制测试方法的执行顺序。这使得测试流程从基础环境验证到上层业务逻辑验证，
 *                      层层递进，更具逻辑性、可读性和问题定位的便捷性。
 */
@SpringBootTest(classes = BlogApplication.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@DisplayName("✅ 核心框架集成测试套件")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FrameworkIntegrationTest {

    /**
     * 注意：这里的日志记录器必须是 org.slf4j.Logger 类型。
     * SLF4J 是一个日志门面，底层的具体实现可以是 Logback、Log4j2 等。
     * 当底层实现是 Logback 时，我们可以通过 SLF4J 的工厂类安全地转换为 Logback 的具体类型，以便进行深度测试。
     */
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(FrameworkIntegrationTest.class);

    // =========== 成员变量：注入测试所需的工具和依赖 ===========

    /**
     * 使用Mockito模拟 {@link DdlInitializer}。
     * <p>
     * {@code @MockitoBean} 注解会在Spring容器中用一个“假的”、无任何操作的Bean来替换真实的 {@code DdlInitializer} Bean。
     * 这样做的【原因】是，我们不希望在运行单元测试或集成测试时，自动执行数据库的DDL（数据定义语言，如建表、删表）操作。
     * 这能使测试环境更加可控、稳定，并避免潜在的测试数据丢失风险。
     */
    @MockitoBean
    private DdlInitializer ddlInitializer;

    /**
     * 自动注入由 {@code @AutoConfigureMockMvc} 配置好的 {@link MockMvc} 实例。
     * {@code @Autowired} 是Spring框架的核心注解，用于在此处自动完成依赖注入。
     * {@code MockMvc} 是我们测试Web层的“瑞士军刀”，可以模拟发送GET, POST等各类HTTP请求，并对响应进行详细的断言。
     */
    @Resource
    private MockMvc mockMvc;

    /**
     * 业务逻辑服务层接口，从Spring容器中注入，用于后续的业务逻辑测试。
     * 它的成功注入，本身就是对Service层及其依赖能否被Spring正确管理的验证。
     */
    @Resource
    private ITestService testService;

    /**
     * 用于JSON序列化和反序列化的工具，由Spring Boot的Jackson自动配置提供并注入。
     * 在测试中，它常用于将对象转换为JSON字符串以便打印日志，或是在构建请求体时使用。
     */
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 注入MyBatis-Plus的核心拦截器Bean ({@link MybatisPlusInterceptor})。
     * 我们注入它的目的是为了在测试中直接检查其内部配置，验证我们在 {@code MybatisPlusConfig} 中定义的插件是否都已正确加载。
     */
    @Resource
    private MybatisPlusInterceptor mybatisPlusInterceptor;

    // =========== 测试方法（按 @Order 顺序执行） ===========

    /**
     * 【第1步】测试Spring应用上下文是否能成功加载，并验证所有核心Bean是否被正确注入。
     * <p>
     * 这是一个基础的“冒烟测试”（Smoke Test）。如果这个测试失败，通常意味着项目的Bean配置、依赖关系或组件扫描存在严重问题，
     * 后续的所有测试都无法进行。因此，它被置于所有测试的最开始。
     * {@code @Order(1)} 确保这是第一个执行的测试。
     */
    @Test
    @Order(1)
    @DisplayName("1. 应用上下文加载与核心Bean注入测试")
    void contextLoadsAndBeansInjected() {
        log.info("▶️ 开始测试 (1/5): 应用上下文加载与核心Bean注入...");
        // 使用 assertAll 可以将多个相关的断言组合在一起。即使其中一个失败，其他的断言也会被执行，最终一次性报告所有失败信息。
        assertAll("核心Bean注入检查",
                () -> assertNotNull(testService, "ITestService 应该被成功注入，不能为null"),
                () -> assertNotNull(mockMvc, "MockMvc 应该被成功注入，不能为null"),
                () -> assertNotNull(objectMapper, "ObjectMapper 应该被成功注入，不能为null"),
                () -> assertNotNull(mybatisPlusInterceptor, "MybatisPlusInterceptor 应该被成功注入，不能为null")
        );
        log.info("✅ 测试通过 (1/5): 应用上下文加载成功，所有核心Bean均已正确注入。");
    }

    /**
     * 【第2步】验证 Logback 日志配置在 'test' 环境下是否按预期加载。
     * <p>
     * 一个稳定、可预测的日志系统是生产环境应用可观测性（Observability）的基石。
     * 该测试会深入 Logback 内部，检查 Root Logger 的级别和挂载的 Appender（输出目的地），
     * 确保日志输出的策略（如控制台输出、异步文件写入、错误日志分离）符合 {@code logback-spring.xml} 中的设计。
     */
    @Test
    @Order(2)
    @DisplayName("2. 日志系统 (Logback) 配置加载测试")
    void testLoggingConfiguration() {
        log.info("▶️ 开始测试 (2/5): 日志系统 (Logback) 配置加载...");

        // 通过SLF4J的LoggerFactory获取底层的具体日志实现工厂，并强制转换为Logback的LoggerContext。这是深入检查Logback配置的入口。
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        // 获取根日志记录器（Root Logger），它是所有logger的顶级父节点。
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

        log.info("   - 步骤1: 验证 Root Logger 的日志级别。根据 logback-spring.xml，'test' profile未指定，应回退到默认的INFO级别。");
        assertEquals(Level.INFO, rootLogger.getLevel(), "在 'test' profile下, Root Logger 的级别应为 INFO");

        log.info("   - 步骤2: 验证 Root Logger 挂载的 Appender 数量和名称。");
        List<String> appenderNames = getAppenderNames(rootLogger);
        assertAll("Root Logger Appender 验证",
                () -> assertEquals(3, appenderNames.size(), "Root Logger 应挂载 3 个 Appender"),
                () -> assertTrue(appenderNames.contains("CONSOLE"), "应包含 CONSOLE Appender"),
                () -> assertTrue(appenderNames.contains("ASYNC_FILE"), "应包含 ASYNC_FILE Appender"),
                () -> assertTrue(appenderNames.contains("ERROR_FILE"), "应包含 ERROR_FILE Appender")
        );

        log.info("   - 步骤3: 深度验证 'ERROR_FILE' Appender 的过滤器行为，确保它只接受ERROR级别的日志。");
        Appender<ch.qos.logback.classic.spi.ILoggingEvent> errorAppender = rootLogger.getAppender("ERROR_FILE");
        assertNotNull(errorAppender, "ERROR_FILE appender 实例不应为 null");

        // 使用 getFirst() 替代 get(0)，代码更现代、更具表达力。
        Filter<ch.qos.logback.classic.spi.ILoggingEvent> firstFilter = errorAppender.getCopyOfAttachedFiltersList().getFirst();

        // 使用 assertInstanceOf 替代 assertTrue，这是 JUnit 5 的推荐用法，断言更精确，失败信息更友好。
        // 同时，它会返回一个已转换类型的实例，方便后续直接调用其方法。
        LevelFilter levelFilter = assertInstanceOf(LevelFilter.class, firstFilter, "ERROR_FILE 的过滤器应为 LevelFilter 类型");

        // 通过测试过滤器的实际行为来验证其配置，而不是试图获取其内部状态。这是更健壮的测试方法。
        log.info("     - 模拟一个ERROR级别的日志事件，预期被过滤器接受(ACCEPT)。");
        assertEquals(FilterReply.ACCEPT, levelFilter.decide(createLoggingEvent(Level.ERROR)), "LevelFilter 应接受 ERROR 级别的事件");

        log.info("     - 模拟一个INFO级别的日志事件，预期被过滤器拒绝(DENY)。");
        assertEquals(FilterReply.DENY, levelFilter.decide(createLoggingEvent(Level.INFO)), "LevelFilter 应拒绝 INFO 级别的事件");

        log.info("✅ 测试通过 (2/5): 日志系统配置正确加载，Root Logger 级别和 Appender 配置及行为均符合预期。");
    }

    /**
     * 【第3步】验证MyBatis-Plus插件是否按预期配置并加载。
     * <p>
     * 检查 {@code MybatisPlusConfig} 中配置的分页、乐观锁和防全表攻击插件是否都已成功注册到拦截器链中。
     * 这确保了框架提供的核心持久层功能（如自动分页）是可用的。
     */
    @Test
    @Order(3)
    @DisplayName("3. MyBatis-Plus 插件配置加载测试")
    void testMybatisPlusPluginsConfiguration() {
        log.info("▶️ 开始测试 (3/5): MyBatis-Plus 插件配置加载...");
        var interceptors = mybatisPlusInterceptor.getInterceptors();

        assertEquals(3, interceptors.size(), "应已配置3个内部拦截器（分页、乐观锁、防全表攻击）");

        assertAll("MyBatis-Plus插件类型验证",
                // 使用 Stream API 的 anyMatch 方法来检查集合中是否存在指定类型的实例，代码简洁且意图明确。
                () -> assertTrue(interceptors.stream().anyMatch(i -> i instanceof PaginationInnerInterceptor), "分页插件(PaginationInnerInterceptor)必须被加载"),
                () -> assertTrue(interceptors.stream().anyMatch(i -> i instanceof OptimisticLockerInnerInterceptor), "乐观锁插件(OptimisticLockerInnerInterceptor)必须被加载"),
                () -> assertTrue(interceptors.stream().anyMatch(i -> i instanceof BlockAttackInnerInterceptor), "防全表更新/删除插件(BlockAttackInnerInterceptor)必须被加载")
        );
        log.info("✅ 测试通过 (3/5): MyBatis-Plus 核心插件已全部正确加载。");
    }

    /**
     * 【第4步】全流程测试基础服务和对象转换器。
     * <p>
     * 此测试模拟了一个最核心的“增查”业务场景，验证了从DTO到Entity的保存、再从数据库查询并转换为VO的完整链路：
     * <strong>DTO -> Service -> Entity -> Mapper -> Database -> Mapper -> Entity -> Service -> VO</strong>。
     * 它的通过，证明了Service层、Mapper层、MyBatis-Plus以及对象转换逻辑是能够正确协同工作的。
     */
    @Test
    @Order(4)
    @DisplayName("4. 基础服务与对象转换全流程 (DTO -> Entity -> DB -> VO)")
    void testBaseServiceAndConverterFlow() throws JsonProcessingException {
        log.info("▶️ 开始测试 (4/5): 基础服务与对象转换全流程...");

        log.info("   - 步骤1: 准备测试数据 (TestDTO)，模拟来自前端的数据");
        TestDTO dto = new TestDTO();
        dto.setName("test-name-from-integration-test");

        log.info("   - 步骤2: 调用 'saveByDto' 将 DTO 保存至数据库");
        Serializable newId = testService.saveByDto(dto);
        assertNotNull(newId, "通过 DTO 保存操作后，应返回一个非 null 的新实体 ID");
        log.info("   - 验证成功: DTO 已保存，获得新实体 ID: {}", newId);

        log.info("   - 步骤3: 调用 'listVo' 从数据库中查询，验证数据已成功持久化");
        TestVO savedVo = testService.listVo(null).stream()
                .filter(vo -> "test-name-from-integration-test".equals(vo.getName()))
                .findFirst()
                .orElse(null);

        // 这是一个健壮的测试实践：先断言对象不为null，再进行后续的属性断言，避免因null导致测试中断。
        assertAll("验证保存后的VO对象",
                () -> assertNotNull(savedVo, "保存后应该能查询到数据"),
                () -> assertNotNull(savedVo != null ? savedVo.getId() : null, "保存后 VO 应该包含 ID"),
                () -> assertEquals("test-name-from-integration-test", savedVo != null ? savedVo.getName() : null, "保存的名称应该匹配")
        );

        log.info("   - 步骤4: 调用 'getVoById' 使用ID精确查找，并验证转换器");
        TestVO foundVo = testService.getVoById(savedVo.getId()).orElse(null);

        assertEquals(savedVo.getId(), foundVo != null ? foundVo.getId() : null, "通过ID查找到的VO的ID应该与保存时的一致");
        log.info("   - 验证成功: 'getVoById' 查找到的VO: {}", objectMapper.writeValueAsString(foundVo));
        log.info("✅ 测试通过 (4/5): 基础服务与对象转换全流程运行通畅。");
    }

    /**
     * 【第5步】测试全局异常处理器是否能正确捕获业务异常并返回统一的JSON响应格式。
     * <p>
     * 通过 {@code MockMvc} 模拟一个必定会抛出指定业务异常的API请求，
     * 然后断言响应的HTTP状态码、业务状态码、消息和数据结构是否符合全局统一的API响应规范。
     * 这是保证API健壮性和客户端友好性的重要一环。
     */
    @Test
    @Order(5)
    @DisplayName("5. 全局异常处理器与统一响应格式测试")
    void testGlobalExceptionHandler() throws Exception {
        log.info("▶️ 开始测试 (5/5): 全局异常处理器...");
        log.info("   - 模拟客户端请求一个会抛出业务异常的接口: GET /framework-test/error");
        mockMvc.perform(get("/framework-test/error"))
                .andExpect(status().isOk()) // 即使业务失败，由于我们正确处理了异常，HTTP状态码也应是200 OK
                .andExpect(jsonPath("$.code").value(SystemErrorCode.SYSTEM_ERROR.getCode())) // 验证业务错误码
                .andExpect(jsonPath("$.message").value(SystemErrorCode.SYSTEM_ERROR.getMessage())) // 验证错误消息
                .andExpect(jsonPath("$.data").isEmpty()) // 验证数据字段为空
                .andDo(print());

        log.info("✅ 测试通过 (5/5): 全局异常处理器成功捕获异常并返回了预期的统一JSON格式。");
    }

    /**
     * 【第6步】测试全局异常处理器在处理参数校验时的【健壮性】。
     * <p>
     * 这是对我们之前优化的直接验证。此测试模拟一个非常边缘但关键的场景：
     * 当一个字段的校验错误信息{@code defaultMessage}为{@code null}时，系统是否会抛出{@code NullPointerException}。
     * <p>
     * 预期行为：得益于我们的健壮性优化 ({@code message == null ? "" : message})，
     * 即使消息为{@code null}，{@code GlobalExceptionHandler}也应能优雅处理，
     * 并在返回的JSON中将该字段的值映射为一个空字符串 {@code ""}。
     *
     * @throws Exception MockMvc 抛出的异常
     */
    @Test
    @Order(6)
    @DisplayName("6. 全局异常处理器 - 参数校验健壮性测试 (处理null消息)")
    void testValidationExceptionHandler_RobustnessForNullMessage() throws Exception {
        log.info("▶️ 开始测试 (6/6): 全局异常处理器 - 参数校验健壮性...");
        // 1. 构造一个包含多种校验失败场景的DTO
        ValidationTestDTO dto = new ValidationTestDTO();
        dto.setNormalField(""); // 触发 @NotBlank，会产生一个标准的、非null的错误消息
        dto.setFieldWithNullMessage("trigger-null-message"); // 触发自定义校验，会产生一个null的错误消息
        // 2. 将DTO对象转换为JSON字符串
        String requestBody = objectMapper.writeValueAsString(dto);
        log.info("   - 模拟客户端POST请求 /framework-test/validation，Body: {}", requestBody);
        log.info("   - 预期: 捕获 MethodArgumentNotValidException, 即使某字段的错误消息为null, 也应正确处理并返回结构化数据");
        // 3. 执行MockMvc请求并进行断言
        mockMvc.perform(post("/framework-test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest()) // 参数校验失败，返回 400
                .andExpect(jsonPath("$.code").value(SystemErrorCode.VALIDATION_ERROR.getCode())) // 业务码
                .andExpect(jsonPath("$.message").value(SystemErrorCode.VALIDATION_ERROR.getMessage())) // 统一消息
                .andExpect(jsonPath("$.data").isMap()) // data 字段是 Map
                // 验证标准校验注解的字段，它的消息是非null的
                .andExpect(jsonPath("$.data.normalField").exists())
                // 核心验证：验证 getDefaultMessage() 返回 null 的字段被正确转换为空字符串 ""
                .andExpect(jsonPath("$.data.fieldWithNullMessage").value(""))
                .andDo(print());
        log.info("✅ 测试通过 (6/6): 全局异常处理器成功处理了包含null错误消息的参数校验异常，程序健壮性得到验证。");
    }

    // =========== 辅助方法 ===========

    /**
     * 辅助方法：获取一个Logger上所有挂载的Appender的名称列表。
     * @param logger Logback Logger 实例
     * @return Appender 名称的列表
     */
    private List<String> getAppenderNames(Logger logger) {
        Iterator<Appender<ch.qos.logback.classic.spi.ILoggingEvent>> appenderIterator = logger.iteratorForAppenders();
        List<String> names = new ArrayList<>();
        appenderIterator.forEachRemaining(appender -> names.add(appender.getName()));
        return names;
    }

    /**
     * 辅助方法：创建一个用于测试的简单日志事件（LoggingEvent）。
     * @param level 日志事件的级别
     * @return 一个模拟的日志事件实例
     */
    private LoggingEvent createLoggingEvent(Level level) {
        LoggingEvent event = new LoggingEvent();
        event.setLevel(level);
        // 可以根据需要设置更多属性，但对于LevelFilter的测试，仅设置Level已足够
        return event;
    }
}
