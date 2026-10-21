package com.blog.architecture.rules;

import com.baomidou.mybatisplus.annotation.TableName;
import com.blog.architecture.ArchitectureTest;
import org.mapstruct.Mapper;

import java.io.Serializable;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * 命名规范规则 - 强制统一风格。
 */
public final class NamingRule {

    private NamingRule() {
    }

    public static void check() {
        // Service 接口（排除责任链处理器子包 ..service.chain..）
        classes().that().resideInAPackage("..service..")
                .and().resideOutsideOfPackage("..service.chain..")
                .and().areInterfaces()
                .should().haveSimpleNameEndingWith("Service")
                .check(ArchitectureTest.CLASSES);

        // Service 实现
        classes().that().resideInAPackage("..service.impl..")
                .and().areNotInterfaces()
                .and().areTopLevelClasses()
                .should().haveSimpleNameEndingWith("Service")
                .orShould().haveSimpleNameEndingWith("ServiceImpl")
                .check(ArchitectureTest.CLASSES);

        // Entity - 使用 @TableName 注解验证且以 Entity 结尾
        classes().that().resideInAPackage("..entity..")
                .and().areNotInterfaces()
                .and().areNotEnums()
                .and().areTopLevelClasses()
                .should().beAnnotatedWith(TableName.class)
                .andShould().haveSimpleNameEndingWith("Entity")
                .because("Entity 必须使用 @TableName 注解映射数据库表且以 Entity 结尾")
                .check(ArchitectureTest.CLASSES);

        // DTO - 命名规范（统一以 DTO 结尾）
        classes().that().resideInAPackage("..dto..")
                .and().areTopLevelClasses()
                .should().haveSimpleNameEndingWith("DTO")
                .because("DTO 类必须统一以 DTO 结尾")
                .check(ArchitectureTest.CLASSES);

        // DTO - 必须实现 Serializable（支持缓存和分布式传输）
        classes().that().resideInAPackage("..dto..")
                .and().haveSimpleNameEndingWith("DTO")
                .should().implement(Serializable.class)
                .because("DTOs 必须实现 Serializable 以支持缓存和分布式传输")
                .check(ArchitectureTest.CLASSES);

        // VO - 命名规范
        classes().that().resideInAPackage("..vo..")
                .and().areTopLevelClasses()
                .should().haveSimpleNameEndingWith("VO")
                .because("VO 类必须以 VO 结尾")
                .check(ArchitectureTest.CLASSES);

        // Mapper/Converter - MapStruct 转换器 (支持两种命名方式)
        classes().that().areAnnotatedWith(Mapper.class)
                .should().resideInAnyPackage("..mapper..", "..converter..")
                .andShould().haveSimpleNameEndingWith("Mapper")
                .orShould().haveSimpleNameEndingWith("Converter")
                .check(ArchitectureTest.CLASSES);

        // 禁止下划线
        classes().should().haveNameNotMatching(".*_.*")
                .check(ArchitectureTest.CLASSES);
    }
}