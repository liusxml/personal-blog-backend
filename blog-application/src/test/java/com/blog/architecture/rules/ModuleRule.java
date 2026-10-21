package com.blog.architecture.rules;

import com.baomidou.mybatisplus.annotation.TableName;
import com.blog.architecture.ArchitectureTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SliceRule;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 模块间依赖规则 - 防止跨模块实现耦合。
 */
public final class ModuleRule {

    public static final SliceRule NO_CYCLE_BETWEEN_MODULES = slices()
            .matching("com.blog.(*)..")
            .should().beFreeOfCycles()
            .because("模块间循环依赖会导致微服务拆分失败");

    public static final ArchRule COMMON_NO_BUSINESS_LOGIC = noClasses()
            .that().resideInAPackage("com.blog.common..")
            .should().beAnnotatedWith(Service.class)
            .orShould().beAnnotatedWith(Repository.class)
            .orShould().beAnnotatedWith(RestController.class)
            .because("common 模块禁止包含业务逻辑");

    /**
     * API 模块纯度检查 - 禁止实现类
     * 确保 API 模块只包含 DTO/VO/Interface/Enum，不包含实现类
     */
    public static final ArchRule API_MODULE_NO_IMPLEMENTATION = noClasses()
            .that().resideInAPackage("..api..")
            .should().beAnnotatedWith(Service.class)
            .orShould().beAnnotatedWith(Repository.class)
            .orShould().beAnnotatedWith(Component.class)
            .orShould().haveSimpleNameContaining("Impl")
            .because("API 模块禁止包含实现类（仅 DTO/VO/Interface/Enum）");

    /**
     * API 模块纯度检查 - 禁止 Entity
     * 确保 API 模块不包含数据库实体，只能使用 DTO/VO
     */
    public static final ArchRule API_MODULE_NO_ENTITY = noClasses()
            .that().resideInAPackage("..api..")
            .should().beAnnotatedWith(TableName.class)
            .because("API 模块禁止包含 Entity（仅 DTO/VO）");

    private ModuleRule() {
    }

    /**
     * 跨模块隔离检查：任一业务模块禁止直接依赖其他业务模块的非 API 包（controller/service/domain/infrastructure/application）
     */
    public static void checkNoCrossModuleImplDependency() {
        ArchitectureTest.BUSINESS_MODULES.forEach(current -> {
            String currentModulePkg = String.format("com.blog.%s..", current);
            ArchitectureTest.BUSINESS_MODULES.stream()
                    .filter(other -> !other.equals(current))
                    .forEach(other -> {
                        String[] forbiddenOtherPackages = {
                                String.format("com.blog.%s.controller..", other),
                                String.format("com.blog.%s.service..", other),
                                String.format("com.blog.%s.domain..", other),
                                String.format("com.blog.%s.infrastructure..", other),
                                String.format("com.blog.%s.application..", other)
                        };
                        noClasses()
                                .that().resideInAPackage(currentModulePkg)
                                .should().dependOnClassesThat().resideInAnyPackage(forbiddenOtherPackages)
                                .because(String.format("%s 模块禁止直接依赖 %s 模块的内部实现，跨模块调用必须通过 %s-api", current, other, other))
                                .check(ArchitectureTest.CLASSES);
                    });
        });
    }

    public static void check() {
        checkNoCrossModuleImplDependency();
        NO_CYCLE_BETWEEN_MODULES.check(ArchitectureTest.CLASSES);
        COMMON_NO_BUSINESS_LOGIC.check(ArchitectureTest.CLASSES);
        API_MODULE_NO_IMPLEMENTATION.check(ArchitectureTest.CLASSES);
        API_MODULE_NO_ENTITY.check(ArchitectureTest.CLASSES);
    }
}