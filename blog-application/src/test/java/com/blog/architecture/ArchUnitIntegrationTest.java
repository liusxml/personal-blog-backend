package com.blog.architecture; // 注意：这个测试类放在 architecture 包下，与其规则同源

import com.blog.architecture.rules.DesignPatternRule;
import com.blog.architecture.rules.LayerRule;
import com.blog.architecture.rules.ModuleRule;
import com.blog.architecture.rules.NamingRule;
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
 *      本测试类将直接引用 {@link ArchitectureTest#CLASSES} 以避免重复导入。
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
        log.info("▶️ ArchUnit 测试初始化: 正在加载待检查的 Java 类...");
        // 直接引用 ArchitectureTest 中已导入的 JavaClasses
        importedClasses = ArchitectureTest.CLASSES;
        log.info("✅ ArchUnit 测试初始化完成: 共加载 {} 个类进行架构检查。", importedClasses.size());
    }

    /**
     * 【第1步】测试分层架构规则（Layered Architecture Rules）。
     * 验证 Controller -> Service -> Repository -> Entity. DTO 等分层依赖是否严格遵守。
     */
    @Test
    @Order(1)
    @DisplayName("1. 验证分层架构规则")
    void testLayerRules() {
        log.info("▶️ 开始测试 (1/4): 分层架构规则...");
        LayerRule.LAYERED_ARCHITECTURE.check(importedClasses);
        LayerRule.CONTROLLERS_IN_CORRECT_PACKAGE.check(importedClasses);
        LayerRule.SERVICES_IN_IMPL_PACKAGE.check(importedClasses);
        LayerRule.REPOSITORIES_IN_CORRECT_PACKAGE.check(importedClasses);
        log.info("✅ 测试通过 (1/4): 分层架构规则全部遵守。");
    }

    /**
     * 【第2步】测试模块间依赖规则（Module Dependency Rules）。
     * 验证模块间没有循环依赖，并且业务模块不依赖其他模块的实现细节。
     */
    @Test
    @Order(2)
    @DisplayName("2. 验证模块间依赖规则")
    void testModuleRules() {
        log.info("▶️ 开始测试 (2/4): 模块间依赖规则...");
        // 移除 assertDoesNotThrow
        ModuleRule.checkNoCrossModuleImplDependency();
        ModuleRule.NO_CYCLE_BETWEEN_MODULES.check(importedClasses);
        ModuleRule.COMMON_NO_BUSINESS_LOGIC.check(importedClasses);
        log.info("✅ 测试通过 (2/4): 模块间依赖规则全部遵守。");
    }

    /**
     * 【第3步】测试命名规范规则（Naming Convention Rules）。
     * 验证 Service、Mapper、Entity、DTO 等类的命名是否符合预期以及禁止下划线。
     */
    @Test
    @Order(3)
    @DisplayName("3. 验证命名规范规则")
    void testNamingRules() {
        log.info("▶️ 开始测试 (3/4): 命名规范规则...");
        // NamingRule 的 check 方法会执行所有命名规则
        NamingRule.check();
        log.info("✅ 测试通过 (3/4): 命名规范规则全部遵守。");
    }

    /**
     * 【第4步】测试设计模式和通用编码规则（Design Pattern & General Coding Rules）。
     * 验证 MapStruct 的使用、禁止泛型异常以及其他通用编码规范。
     */
    @Test
    @Order(4)
    @DisplayName("4. 验证设计模式与通用编码规则")
    void testDesignPatternAndGeneralCodingRules() {
        log.info("▶️ 开始测试 (4/4): 设计模式与通用编码规则...");
        // DesignPatternRule 的 check 方法会执行所有设计模式和通用编码规则
        DesignPatternRule.check();
        log.info("✅ 测试通过 (4/4): 设计模式与通用编码规则全部遵守。");
    }

    // 您也可以为单个特别复杂的 ArchRule 定义一个独立的测试方法，
    // 例如，如果某个规则容易出错或经常被违反，单独测试它会更有帮助。
    @Test
    @Order(5)
    @DisplayName("5. 独立验证 Service 层禁止手动映射")
    void testNoManualMappingInServiceIsolated() {
        log.info("▶️ 开始测试 (5/X): 独立验证 Service 层禁止手动映射...");

        DesignPatternRule.NO_MANUAL_MAPPING_IN_SERVICE.check(importedClasses);
        log.info("✅ 测试通过 (5/X): Service 层禁止手动映射规则遵守。");
    }

    /**
     * 【第6步】测试 API 规范规则（API Standards Rules）。
     * 验证 Controller 返回值类型、Entity 暴露等 API 设计规范。
     */
    @Test
    @Order(6)
    @DisplayName("6. 验证 API 规范规则")
    void testApiRules() {
        log.info("▶️ 开始测试 (6/X): API 规范规则...");
        com.blog.architecture.rules.ApiRule.check();
        log.info("✅ 测试通过 (6/X): API 规范规则全部遵守。");
    }
}
