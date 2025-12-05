package com.blog.architecture.rules;

import com.blog.architecture.ArchitectureTest;

import java.io.Serializable;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * 命名规范规则 - 强制统一风格。
 */
public final class NamingRule {

        private NamingRule() {
        }

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

                // Entity - 使用 @TableName 注解验证（更灵活，符合项目实际）
                classes().that().resideInAPackage("..entity..")
                                .and().areNotInterfaces()
                                .and().areNotEnums()
                                .should().beAnnotatedWith(com.baomidou.mybatisplus.annotation.TableName.class)
                                .because("Entity 必须使用 @TableName 注解映射数据库表")
                                .check(ArchitectureTest.CLASSES);

                // DTO - 命名规范
                classes().that().resideInAPackage("..dto..")
                                .should().haveSimpleNameEndingWith("DTO")
                                .check(ArchitectureTest.CLASSES);

                // DTO - 必须实现 Serializable（支持缓存和分布式传输）
                classes().that().resideInAPackage("..dto..")
                                .and().haveSimpleNameEndingWith("DTO")
                                .should().implement(Serializable.class)
                                .because("DTOs 必须实现 Serializable 以支持缓存和分布式传输")
                                .check(ArchitectureTest.CLASSES);

                // Mapper/Converter - MapStruct 转换器 (支持两种命名方式)
                classes().that().areAnnotatedWith(org.mapstruct.Mapper.class)
                                .should().resideInAnyPackage("..mapper..", "..converter..")
                                .andShould().haveSimpleNameEndingWith("Mapper")
                                .orShould().haveSimpleNameEndingWith("Converter")
                                .check(ArchitectureTest.CLASSES);

                // 禁止下划线
                classes().should().haveNameNotMatching(".*_.*")
                                .check(ArchitectureTest.CLASSES);
        }
}