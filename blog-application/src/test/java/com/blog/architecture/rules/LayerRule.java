package com.blog.architecture.rules;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.blog.architecture.config.ArchUnitConfig.CONTROLLER_PKG;
import static com.blog.architecture.config.ArchUnitConfig.DTO_PKG;
import static com.blog.architecture.config.ArchUnitConfig.ENTITY_PKG;
import static com.blog.architecture.config.ArchUnitConfig.REPOSITORY_PKG;
import static com.blog.architecture.config.ArchUnitConfig.SERVICE_IMPL_PKG;
import static com.blog.architecture.config.ArchUnitConfig.SERVICE_PKG;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * 分层架构规则 - 强制单向依赖（Controller → Service → Repository → Entity）。
 */
public final class LayerRule {

    private LayerRule() {}

    /** 严格分层依赖 */
    public static final ArchRule LAYERED_ARCHITECTURE = layeredArchitecture()
            .consideringAllDependencies()
            .layer("Controller").definedBy(CONTROLLER_PKG)
            .layer("Service").definedBy(SERVICE_PKG)
            .layer("Repository").definedBy(REPOSITORY_PKG)
            .layer("Entity").definedBy(ENTITY_PKG)
            .layer("DTO").definedBy(DTO_PKG)

            .whereLayer("Controller").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Service")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
            .whereLayer("Entity").mayOnlyBeAccessedByLayers("Repository", "Service")
            .whereLayer("DTO").mayOnlyBeAccessedByLayers("Controller", "Service", "Repository", "DTO");

    /** 注解位置约束 */
    public static final ArchRule CONTROLLERS_IN_CORRECT_PACKAGE = classes()
            .that().areAnnotatedWith(RestController.class)
            .or().areAnnotatedWith(Controller.class)
            .should().resideInAPackage(CONTROLLER_PKG)
            .because("控制器必须位于 controller 包");

    public static final ArchRule SERVICES_IN_IMPL_PACKAGE = classes()
            .that().areAnnotatedWith(Service.class)
            .should().resideInAPackage(SERVICE_IMPL_PKG)
            .because("服务实现类必须位于 impl 子包");

    public static final ArchRule REPOSITORIES_IN_CORRECT_PACKAGE = classes()
            .that().areAnnotatedWith(Repository.class)
            .should().resideInAPackage(REPOSITORY_PKG)
            .because("仓库类必须位于 repository 或 mapper 包");

    /** 执行所有规则 */
    public static void check(JavaClasses classes) {
        LAYERED_ARCHITECTURE.check(classes);
        CONTROLLERS_IN_CORRECT_PACKAGE.check(classes);
        SERVICES_IN_IMPL_PACKAGE.check(classes);
        REPOSITORIES_IN_CORRECT_PACKAGE.check(classes);
    }
}