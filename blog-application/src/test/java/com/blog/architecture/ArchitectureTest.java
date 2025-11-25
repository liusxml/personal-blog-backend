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
 * ArchUnit 全局测试入口 - 统一管理类导入、缓存与过滤策略。
 * <p>
 * 设计模式：模板方法模式（Template Method）<br>
 * 所有规则类通过继承或组合使用 {@link #CLASSES} 避免重复扫描。
 * </p>
 * <p>
 * 性能优化：使用 {@link CacheMode#PER_CLASS} + 自定义过滤器排除生成代码。
 * </p>
 * <p>
 * 微服务预留：包结构与模块划分清晰，未来可按模块拆分独立测试。
 * </p>
 *
 * @author ArchUnit Team
 * @since 1.0
 */
@AnalyzeClasses(
        packages = "com.blog",
        cacheMode = CacheMode.PER_CLASS,
        importOptions = {
                ImportOption.DoNotIncludeTests.class,
                ImportOption.DoNotIncludeJars.class,
                ArchitectureTest.GeneratedCodeFilter.class
        }
)
public class ArchitectureTest {

    /**
     * 缓存的生产代码类集合 - 所有规则复用
     */
    public static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new GeneratedCodeFilter())
            .importPackages("com.blog");

    /**
     * 项目业务模块名 - 使用 Guava 不可变集合
     */

    public static final ImmutableSet<String> BUSINESS_MODULES = ImmutableSet.copyOf(ArchUnitConfig.BUSINESS_MODULE_NAMES);


    /**
     * 自定义过滤器 - 排除 Lombok、MapStruct、generated 目录。
     * <p>使用 {@link FilenameUtils} 安全处理路径，避免 NPE。</p>
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