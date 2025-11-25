package com.blog.architecture.rules;

import com.blog.architecture.ArchitectureTest;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * 命名规范规则 - 强制统一风格。
 */
public final class NamingRule {

    private NamingRule() {}

    public static void check() {
        // Service 接口
        classes().that().resideInAPackage("..service..")
                .and().areInterfaces()
                .should().haveSimpleNameEndingWith("Service")
                .check(ArchitectureTest.CLASSES);

        // Service 实现
        classes().that().resideInAPackage("..service.impl..")
                .and().areNotInterfaces()
                .should().haveSimpleNameEndingWith("Service")
                .orShould().haveSimpleNameEndingWith("ServiceImpl")
                .check(ArchitectureTest.CLASSES);

        // Entity
        classes().that().resideInAnyPackage("..entity..", "..domain..")
                .should().haveSimpleNameEndingWith("Entity")
                .orShould().haveSimpleNameEndingWith("DO")
                .check(ArchitectureTest.CLASSES);

        // DTO
        classes().that().resideInAPackage("..dto..")
                .should().haveSimpleNameEndingWith("DTO")
                .check(ArchitectureTest.CLASSES);

        // Mapper
        classes().that().areAnnotatedWith(org.mapstruct.Mapper.class)
                .should().resideInAPackage("..mapper..")
                .andShould().haveSimpleNameEndingWith("Mapper")
                .check(ArchitectureTest.CLASSES);

        // 禁止下划线
        classes().should().haveNameNotMatching(".*_.*")
                .check(ArchitectureTest.CLASSES);
    }
}