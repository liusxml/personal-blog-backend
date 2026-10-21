package com.blog.architecture.rules;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.blog.architecture.config.ArchUnitConfig.APPLICATION_PKG;
import static com.blog.architecture.config.ArchUnitConfig.CONTROLLER_PKG;
import static com.blog.architecture.config.ArchUnitConfig.CONVERTER_PKG;
import static com.blog.architecture.config.ArchUnitConfig.DTO_PKG;
import static com.blog.architecture.config.ArchUnitConfig.ENTITY_PKG;
import static com.blog.architecture.config.ArchUnitConfig.MAPPER_PKG;
import static com.blog.architecture.config.ArchUnitConfig.REPOSITORY_PKG;
import static com.blog.architecture.config.ArchUnitConfig.SERVICE_IMPL_PKG;
import static com.blog.architecture.config.ArchUnitConfig.SERVICE_PKG;
import static com.blog.architecture.config.ArchUnitConfig.VO_PKG;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * 分层架构规则 - 强制单向依赖（Controller → Service → Persistence/Converter → Entity）。
 */
public final class LayerRule {

    /**
     * 严格分层依赖（仅检查归属于分层内的依赖关系，排除 JDK / 框架基础类干扰）
     */
    public static final ArchRule LAYERED_ARCHITECTURE = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Controller").definedBy(CONTROLLER_PKG)
            .layer("Service").definedBy(SERVICE_PKG, APPLICATION_PKG)
            .layer("Persistence").definedBy(REPOSITORY_PKG, MAPPER_PKG)
            .layer("Converter").definedBy(CONVERTER_PKG)
            .layer("Entity").definedBy(ENTITY_PKG)
            .layer("DTO").definedBy(DTO_PKG, VO_PKG)

            .whereLayer("Controller").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Service")
            .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service")
            .whereLayer("Converter").mayOnlyBeAccessedByLayers("Service", "Converter")
            .whereLayer("Entity").mayOnlyBeAccessedByLayers("Persistence", "Service", "Converter", "Entity")
            .whereLayer("DTO").mayOnlyBeAccessedByLayers("Controller", "Service", "Persistence", "Converter", "DTO");

    /**
     * 注解位置约束：所有控制器必须位于 controller 包
     */
    public static final ArchRule CONTROLLERS_IN_CORRECT_PACKAGE = classes()
            .that().areAnnotatedWith(RestController.class)
            .or().areAnnotatedWith(Controller.class)
            .should().resideInAPackage(CONTROLLER_PKG)
            .because("控制器必须位于 controller 包");

    /**
     * 服务实现类位置约束：所有 @Service 实现类必须位于 service.impl 子包
     */
    public static final ArchRule SERVICES_IN_IMPL_PACKAGE = classes()
            .that().areAnnotatedWith(Service.class)
            .should().resideInAPackage(SERVICE_IMPL_PKG)
            .because("所有 @Service 服务实现类必须位于 service.impl 子包");

    /**
     * 持久层位置约束：@Repository 或 MyBatis @Mapper 必须位于 repository 或 mapper 包
     */
    public static final ArchRule REPOSITORIES_IN_CORRECT_PACKAGE = classes()
            .that().areAnnotatedWith(Repository.class)
            .or().areAnnotatedWith(Mapper.class)
            .should().resideInAnyPackage(REPOSITORY_PKG, MAPPER_PKG)
            .because("持久层接口/仓库类必须位于 repository 或 mapper 包");

    /**
     * Controller 禁止位于 blog-application 模块
     */
    public static final ArchRule NO_CONTROLLERS_IN_APPLICATION = noClasses()
            .that().areAnnotatedWith(RestController.class)
            .or().areAnnotatedWith(Controller.class)
            .should().resideInAnyPackage("com.blog.application..", "com.blog.config..", "com.blog.handler..")
            .because("Controllers 必须位于 *-service 模块，blog-application 仅用于启动类和全局配置");

    private LayerRule() {
    }

    /**
     * 执行所有规则
     */
    public static void check(JavaClasses classes) {
        LAYERED_ARCHITECTURE.check(classes);
        CONTROLLERS_IN_CORRECT_PACKAGE.check(classes);
        SERVICES_IN_IMPL_PACKAGE.check(classes);
        REPOSITORIES_IN_CORRECT_PACKAGE.check(classes);
        NO_CONTROLLERS_IN_APPLICATION.check(classes);
    }
}