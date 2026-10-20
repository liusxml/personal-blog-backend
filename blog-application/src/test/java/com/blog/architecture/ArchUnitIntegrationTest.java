package com.blog.architecture; // 注意：这个测试类放在 architecture 包下，与其规则同源

import com.blog.architecture.rules.ApiRule;
import com.blog.architecture.rules.DesignPatternRule;
import com.blog.architecture.rules.LayerRule;
import com.blog.architecture.rules.ModuleRule;
import com.blog.architecture.rules.NamingRule;
import com.blog.architecture.rules.PlantUMLRule;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ArchUnit 架构规则集成测试套件 v1.0
 * <p>
 * 这份测试类用于验证项目定义的各项架构规则（包括分层、模块依赖、命名规范和设计模式）
 * 在当前代码库中是否得到严格遵守。它通过执行 ArchUnit 定义的规则，
 * 确保代码库未发生架构腐化，并为未来的重构和新功能开发提供自动化约束。
 * </p>
 * <p>
 * **核心理念**：将架构规则视为不可变的测试代码，每次构建时自动验证，发现任何架构违规。
 * </p>
 *
 * @author ArchUnit Team
 * @see ArchitectureTest 所有 ArchUnit 规则所需的 {@link JavaClasses} 都在此进行一次性加载和缓存。
 * 本测试类将直接引用 {@link ArchitectureTest#CLASSES} 以避免重复导入。
 * @see ArchRule ArchUnit 架构规则的基石，定义了代码应遵循的约束。
 * @see BeforeAll 确保在所有测试方法执行前，ArchUnit 所需的类已导入并准备就绪。
 * @since 1.0
 */
@DisplayName("✅ ArchUnit 架构规则集成测试套件")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ArchUnitIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ArchUnitIntegrationTest.class);

    // 静态成员变量，用于存储被扫描的类，确保只加载一次
    private static JavaClasses importedClasses;

    /**
     * 在所有测试方法执行前，加载一次所有需要检查的 Java 类。
     * 这利用了 `ArchitectureTest.CLASSES` 中的已导入和缓存的类，避免重复文件 IO。
     */
    @BeforeAll
    static void setup() {
        log.info("=".repeat(80));
        log.info("🚀 ArchUnit 架构测试套件 - 初始化开始");
        log.info("=".repeat(80));
        log.info("📦 正在加载待检查的 Java 类（来源：ArchitectureTest.CLASSES 缓存）...");

        // 直接引用 ArchitectureTest 中已导入的 JavaClasses
        long startTime = System.currentTimeMillis();
        importedClasses = ArchitectureTest.CLASSES;
        long loadTime = System.currentTimeMillis() - startTime;

        log.info("✅ 类加载完成");
        log.info("   ├─ 类总数: {} 个", importedClasses.size());
        log.info("   ├─ 加载耗时: {} ms（得益于缓存机制）", loadTime);
        log.info("   └─ 业务模块: {}", ArchitectureTest.BUSINESS_MODULES);
        log.info("=".repeat(80));
    }

    /**
     * 【第1步】测试分层架构规则（Layered Architecture Rules）。
     * 验证 Controller -> Service -> Persistence/Converter -> Entity 等分层依赖是否严格遵守。
     */
    @Test
    @Order(1)
    @DisplayName("1. 验证分层架构规则")
    void testLayerRules() {
        log.info("");
        log.info("🔍 [测试 1/7] 分层架构规则 (Layered Architecture)");
        log.info("-".repeat(80));
        log.info("📋 验证项:");
        log.info("   ├─ Controller → Service → Persistence/Converter → Entity 依赖方向");
        log.info("   ├─ Controller 必须在 *.controller 包下（且禁止位于 blog-application）");
        log.info("   ├─ 标准 Service 实现必须在 *.service.impl 子包");
        log.info("   └─ Repository/Mapper 必须在正确的包下");

        LayerRule.check(importedClasses);

        log.info("✅ 测试通过: 所有分层架构规则遵守");
    }

    /**
     * 【第2步】测试模块间依赖规则（Module Dependency Rules）。
     * 验证模块间没有循环依赖，并且业务模块不依赖其他模块的实现细节。
     */
    @Test
    @Order(2)
    @DisplayName("2. 验证模块间依赖规则")
    void testModuleRules() {
        log.info("");
        log.info("🔍 [测试 2/7] 模块间依赖规则 (Module Dependencies)");
        log.info("-".repeat(80));
        log.info("📋 验证项:");
        log.info("   ├─ 禁止跨模块非 API 层依赖（跨模块必须走 *-api）");
        log.info("   ├─ 禁止模块间循环依赖");
        log.info("   ├─ Common 模块无业务逻辑");
        log.info("   └─ API 模块纯度检查（禁止实现类与 Entity）");

        ModuleRule.check();

        log.info("✅ 测试通过: 所有模块间依赖规则遵守");
    }

    /**
     * 【第3步】测试命名规范规则（Naming Convention Rules）。
     * 验证 Service、Mapper、Entity、DTO、VO 等类的命名是否符合预期以及禁止下划线。
     */
    @Test
    @Order(3)
    @DisplayName("3. 验证命名规范规则")
    void testNamingRules() {
        log.info("");
        log.info("🔍 [测试 3/7] 命名规范规则 (Naming Conventions)");
        log.info("-".repeat(80));
        log.info("📋 验证项:");
        log.info("   ├─ Service 接口和实现类命名");
        log.info("   ├─ DTO/VO 和 Entity 命名及注解");
        log.info("   ├─ Mapper/Converter 命名");
        log.info("   └─ 禁止类名包含下划线");

        NamingRule.check();

        log.info("✅ 测试通过: 所有命名规范规则遵守");
    }

    /**
     * 【第4步】测试设计模式和通用编码规则（Design Pattern & General Coding Rules）。
     * 验证 MapStruct 的使用、禁止泛型异常以及其他通用编码规范。
     */
    @Test
    @Order(4)
    @DisplayName("4. 验证设计模式与通用编码规则")
    void testDesignPatternAndGeneralCodingRules() {
        log.info("");
        log.info("🔍 [测试 4/7] 设计模式与通用编码规则 (Design Patterns & General Coding)");
        log.info("-".repeat(80));
        log.info("📋 验证项:");
        log.info("   ├─ Service 层禁止手动映射（强制使用 MapStruct BaseConverter）");
        log.info("   ├─ 禁止字段注入（强制构造器注入）");
        log.info("   ├─ 禁止泛型异常（Exception.class/RuntimeException.class）");
        log.info("   ├─ 标准 CRUD Service 实现类必须继承 BaseServiceImpl");
        log.info("   └─ 通用编码规范（禁用 JodaTime、java.util.logging 等）");

        DesignPatternRule.check();

        log.info("✅ 测试通过: 所有设计模式与通用编码规则遵守");
    }

    @Test
    @Order(5)
    @DisplayName("5. 独立验证 Service 层禁止手动映射")
    void testNoManualMappingInServiceIsolated() {
        log.info("");
        log.info("🔍 [测试 5/7] 独立检查: Service 层禁止手动映射");
        log.info("-".repeat(80));
        log.info("📋 重点验证: Service 层必须使用 MapStruct，禁止手写 toDto/toVo/toEntity 或调用 BeanUtils.copyProperties()");

        DesignPatternRule.NO_MANUAL_MAPPING_IN_SERVICE.check(importedClasses);

        log.info("✅ 测试通过: Service 层无手动映射代码");
    }

    /**
     * 【第6步】测试 API 规范规则（API Standards Rules）。
     * 验证 Controller 返回值类型、Entity 暴露等 API 设计规范。
     */
    @Test
    @Order(6)
    @DisplayName("6. 验证 API 规范规则")
    void testApiRules() {
        log.info("");
        log.info("🔍 [测试 6/7] API 规范规则 (API Standards)");
        log.info("-".repeat(80));
        log.info("📋 验证项:");
        log.info("   ├─ Controller 方法必须返回 Result<T>（允许特殊类型白名单）");
        log.info("   └─ Controller 禁止直接或通过泛型返回 Entity（防止数据泄露）");

        ApiRule.check();

        log.info("✅ 测试通过: 所有 API 规范规则遵守");
    }

    /**
     * 【第7步】测试 PlantUML 架构图规则（PlantUML Architecture Diagram Rules）。
     * 验证实际代码架构是否符合 architecture-diagram.puml 中定义的模块依赖关系。
     */
    @Test
    @Order(7)
    @DisplayName("7. 验证 PlantUML 架构图规则")
    void testPlantUMLArchitecture() {
        log.info("");
        log.info("🔍 [测试 7/7] PlantUML 架构图规则 (Architecture Diagram)");
        log.info("-".repeat(80));
        log.info("📋 验证项: 代码依赖关系是否符合 architecture-diagram.puml 中定义的架构约束");
        log.info("📊 架构图位置: {}", PlantUMLRule.getPlantUmlDiagramPath());

        PlantUMLRule.check();

        log.info("✅ 测试通过: PlantUML 架构图规则遵守");
        log.info("");
        log.info("=".repeat(80));
        log.info("🎉 ArchUnit 架构测试套件全部通过！架构规则完全遵守。");
        log.info("=".repeat(80));
    }
}
