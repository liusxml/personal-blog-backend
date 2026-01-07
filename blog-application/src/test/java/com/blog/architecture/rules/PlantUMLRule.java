package com.blog.architecture.rules;

import com.blog.architecture.ArchitectureTest;
import com.tngtech.archunit.lang.ArchRule;

import java.net.URL;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.plantuml.rules.PlantUmlArchCondition.Configuration.consideringAllDependencies;
import static com.tngtech.archunit.library.plantuml.rules.PlantUmlArchCondition.adhereToPlantUmlDiagram;

/**
 * PlantUML 架构图规则 - 使用 ArchUnit 原生PlantUML支持
 * <p>
 * 此规则类使用 ArchUnit 官方的 PlantUML 集成功能，将 architecture-diagram.puml 中定义的
 * 组件依赖关系转换为可执行的架构测试。
 * </p>
 * <p>
 * <b>工作原理</b>：
 * - PlantUML 组件图使用 `<<package.name>>` 语法标记组件对应的 Java 包
 * - ArchUnit 自动解析图表并验证实际代码依赖是否符合图表定义
 * - 使用 `consideringOnlyDependenciesInDiagram()` 只检查图中显式定义的依赖
 * </p>
 * <p>
 * <b>示例</b>：
 *
 * <pre>
 * component [blog-system-service] <<com.blog.system.service>> as sysSvc
 * component [blog-system-api] <<com.blog.system.api>> as sysApi
 *
 * sysSvc --> sysApi : uses
 * </pre>
 * <p>
 * 上述定义会验证 `com.blog.system.service` 包可以依赖 `com.blog.system.api` 包。
 * </p>
 *
 * @author ArchUnit Team
 * @see <a href=
 * "https://www.archunit.org/userguide/html/000_Index.html#_plantuml_component_diagrams_as_rules">ArchUnit
 * PlantUML Documentation</a>
 * @since 1.0
 */
public final class PlantUMLRule {

    /**
     * PlantUML 架构图路径（类路径相对路径）
     */
    private static final String DIAGRAM_PATH = "/architecture-diagram.puml";
    /**
     * PlantUML 架构图规则 - 使用官方 API
     * <p>
     * 此规则会：
     * 1. 加载 architecture-diagram.puml 文件
     * 2. 解析组件定义（通过 <<package>> stereotype 关联代码）
     * 3. 验证实际代码依赖是否符合图表中的箭头（-->）定义
     * 4. 检查所有依赖关系（consideringAllDependencies）- 图中未定义的依赖会引发警告
     * </p>
     * <p>
     * ⚠️ 注意：本规则重点验证<b>模块间依赖关系</b>，不强制要求所有类都在图表中。
     * 图表主要关注业务模块（system、article、comment、file）之间的依赖控制。
     * </p>
     */
    public static final ArchRule ARCHITECTURE_MATCHES_DIAGRAM = classes()
            .should(adhereToPlantUmlDiagram(
                    getDiagramUrl(),
                    consideringAllDependencies()))
            .because("代码架构必须严格遵循 architecture-diagram.puml 中定义的模块依赖关系");

    /**
     * 私有构造器，防止实例化（工具类）
     */
    private PlantUMLRule() {
    }

    /**
     * 架构图 URL（从类路径加载）
     */
    private static URL getDiagramUrl() {
        URL url = PlantUMLRule.class.getResource(DIAGRAM_PATH);
        if (url == null) {
            throw new IllegalStateException(
                    "PlantUML architecture diagram not found at classpath: " + DIAGRAM_PATH +
                            ". Expected location: src/test/resources" + DIAGRAM_PATH);
        }
        return url;
    }

    /**
     * 获取 PlantUML 图表文件路径（用于文档参考和错误消息）
     *
     * @return 图表文件相对路径
     */
    public static String getPlantUmlDiagramPath() {
        return "src/test/resources" + DIAGRAM_PATH;
    }

    /**
     * 验证 PlantUML 图表文件是否存在
     *
     * @return 图表文件是否存在
     */
    public static boolean isPlantUmlDiagramAvailable() {
        return PlantUMLRule.class.getResource(DIAGRAM_PATH) != null;
    }

    /**
     * 统一执行入口（模板方法）
     * <p>
     * 在执行规则前会验证图表文件存在性
     * </p>
     */
    public static void check() {
        if (!isPlantUmlDiagramAvailable()) {
            throw new IllegalStateException(
                    "PlantUML architecture diagram not found. " +
                            "Expected location: " + getPlantUmlDiagramPath());
        }
        ARCHITECTURE_MATCHES_DIAGRAM.check(ArchitectureTest.CLASSES);
    }
}
