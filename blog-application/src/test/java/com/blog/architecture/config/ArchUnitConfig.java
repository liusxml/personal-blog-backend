package com.blog.architecture.config;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Set;

/**
 * ArchUnit 集中配置 - 使用 String 通配符（..）替代 Pattern，提升性能与可读性。
 *
 * <p><strong>设计模式：</strong> 常量配置 + 不可变集合（Guava）</p>
 * <ul>
 *   <li>所有包路径使用 {@code ..} 通配符，兼容 {@code resideInAPackage(String)}。</li>
 *   <li>后期微服务拆分时，只需调整对应模块的包前缀，规则自动适配。</li>
 * </ul>
 *
 * @author ArchUnit Team
 * @since 1.0
 */
public final class ArchUnitConfig {

    /* --------------------------------------------------------------------- *
     * 1. 包路径常量 - 使用 String + ".." 通配符（ArchUnit 推荐）
     * --------------------------------------------------------------------- */
    public static final String CONTROLLER_PKG = "com.blog..controller..";
    public static final String SERVICE_PKG = "com.blog..service..";
    public static final String SERVICE_IMPL_PKG = "com.blog..service.impl..";
    public static final String REPOSITORY_PKG = "com.blog..repository..";
    public static final String MAPPER_PKG = "com.blog..mapper..";
    public static final String ENTITY_PKG = "com.blog..entity..";
    public static final String DTO_PKG = "com.blog..dto..";
    /* --------------------------------------------------------------------- *
     * 2. 模块包前缀映射（用于模块化规则）
     * --------------------------------------------------------------------- */
    public static final Map<String, String> MODULE_PACKAGES = ImmutableMap.of(
            "system", "com.blog.system",
            "article", "com.blog.article",
            "comment", "com.blog.comment",
            "file", "com.blog.file"
    );
    public static final Set<String> BUSINESS_MODULE_NAMES = MODULE_PACKAGES.keySet();
    /* --------------------------------------------------------------------- *
     * 3. 类命名后缀规范（可用于命名规则）
     * --------------------------------------------------------------------- */
    public static final Map<String, String[]> NAMING_SUFFIXES = ImmutableMap.of(
            "interface-service", new String[]{"Service"},
            "class-service-impl", new String[]{"Service", "ServiceImpl"},
            "entity", new String[]{"Entity", "DO"},
            "dto", new String[]{"DTO"},
            "mapper", new String[]{"Mapper"}
    );


    /**
     * 禁止实例化（工具类）
     */
    private ArchUnitConfig() {
    }
}