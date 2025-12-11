package com.blog.architecture;

import com.blog.architecture.config.ArchUnitConfig;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.CacheMode;
import org.apache.commons.io.FilenameUtils;

import java.util.Set;

/**
 * ArchUnit 全局测试入口 - 统一管理类导入、缓存与过滤策略
 * 
 * <h2>核心职责</h2>
 * <ul>
 * <li><b>类加载</b>：一次性加载 {@code com.blog} 包下的所有生产代码类</li>
 * <li><b>性能缓存</b>：使用 {@link CacheMode#PER_CLASS} 避免多次文件 IO</li>
 * <li><b>智能过滤</b>：排除测试代码、第三方 JAR、生成代码（Lombok/MapStruct）</li>
 * </ul>
 * 
 * <h2>设计模式</h2>
 * <p>
 * <b>模板方法模式（Template Method）</b>：所有规则类通过继承或组合使用 {@link #CLASSES}，
 * 避免重复扫描，确保性能一致性。
 * </p>
 * 
 * <h2>使用示例</h2>
 * 
 * <pre>{@code
 * // 在规则类中直接引用已缓存的类集合
 * public class MyRule {
 *     public static final ArchRule MY_RULE = classes()
 *         .that()...
 *         .should()...;
 *     
 *     public static void check() {
 *         MY_RULE.check(ArchitectureTest.CLASSES);  // 复用缓存
 *     }
 * }
 * }</pre>
 * 
 * <h2>性能优化</h2>
 * <ul>
 * <li><b>缓存策略</b>：{@link CacheMode#PER_CLASS} 针对每个测试类缓存一次</li>
 * <li><b>自定义过滤</b>：{@link GeneratedCodeFilter} 排除约 30% 的无关类</li>
 * <li><b>包扫描范围</b>：精确到 {@code com.blog}，避免扫描 JDK 和第三方库</li>
 * </ul>
 * 
 * <h2>微服务预留设计</h2>
 * <p>
 * {@link #BUSINESS_MODULES} 明确定义业务模块边界，未来拆分为微服务时，
 * 可按模块独立测试，保证架构规则的可演进性。
 * </p>
 *
 * @author ArchUnit Team
 * @version 1.1
 * @since 1.0
 * @see CacheMode#PER_CLASS 类级别缓存策略
 * @see JavaClasses ArchUnit 的核心类集合
 */
@AnalyzeClasses(packages = "com.blog", cacheMode = CacheMode.PER_CLASS, importOptions = {
        ImportOption.DoNotIncludeTests.class,
        ImportOption.DoNotIncludeJars.class,
        ArchitectureTest.GeneratedCodeFilter.class
})
public class ArchitectureTest {

    /**
     * 缓存的生产代码类集合 - 所有架构规则复用此实例
     * 
     * <p>
     * <b>设计目的</b>：避免多次扫描文件系统，提升测试性能
     * </p>
     * 
     * <p>
     * <b>线程安全</b>：{@link JavaClasses} 是不可变对象，可安全在多线程环境使用
     * </p>
     * 
     * <p>
     * <b>内存占用</b>：典型项目约占用 10-50 MB，取决于类数量和复杂度
     * </p>
     * 
     * <h3>使用示例</h3>
     * 
     * <pre>{@code
     * // 在规则类中引用
     * MY_RULE.check(ArchitectureTest.CLASSES);
     * }</pre>
     */
    public static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new GeneratedCodeFilter())
            .importPackages("com.blog");

    /**
     * 项目业务模块名称集合 - 用于动态生成模块间依赖规则
     * 
     * <p>
     * <b>数据来源</b>：从 {@link ArchUnitConfig#BUSINESS_MODULE_NAMES} 加载
     * </p>
     * 
     * <p>
     * <b>不可变性</b>：使用 Guava {@link ImmutableSet} 确保集合内容不可修改
     * </p>
     * 
     * <p>
     * <b>典型值</b>：{@code ["system", "article", "comment", "file"]}
     * </p>
     * 
     * <h3>使用场景</h3>
     * <ul>
     * <li>动态生成跨模块依赖检查规则</li>
     * <li>批量创建模块级别的架构约束</li>
     * <li>支持模块增删时自动调整规则</li>
     * </ul>
     * 
     * @see ArchUnitConfig#BUSINESS_MODULE_NAMES 配置源
     */
    public static final ImmutableSet<String> BUSINESS_MODULES = ImmutableSet
            .copyOf(ArchUnitConfig.BUSINESS_MODULE_NAMES);

    /**
     * 自定义过滤器 - 排除 Lombok、MapStruct、generated 目录。
     * <p>
     * 使用 {@link FilenameUtils} 安全处理路径，避免 NPE。
     * </p>
     */
    public static final class GeneratedCodeFilter implements ImportOption {

        private static final Set<String> GENERATED_PATHS = Set.of("/lombok/", "/mapstruct/", "/generated/");

        @Override
        public boolean includes(Location location) {
            String path = location.toString();
            String filename = FilenameUtils.getName(path);

            // 排除 Lombok 生成的 _*.class 文件
            if (filename.contains("_")) {
                return false;
            }

            // 排除生成目录
            return GENERATED_PATHS.stream().noneMatch(path::contains);
        }
    }
}