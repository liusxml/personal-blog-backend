package com.blog.architecture.rules;

import com.blog.architecture.ArchitectureTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SliceRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 模块间依赖规则 - 防止跨模块实现耦合。
 */
public final class ModuleRule {

    private ModuleRule() {}

    public static void checkNoCrossModuleImplDependency() {
        ArchitectureTest.BUSINESS_MODULES.forEach(current -> {
            String currentService = String.format("com.blog.%s.service..", current);
            ArchitectureTest.BUSINESS_MODULES.stream()
                    .filter(other -> !other.equals(current))
                    .forEach(other -> {
                        String otherImpl = String.format("com.blog.%s.service.impl..", other);
                        noClasses()
                                .that().resideInAPackage(currentService)
                                .should().dependOnClassesThat().resideInAPackage(otherImpl)
                                .because(String.format("%s 模块不应依赖 %s 的实现层", current, other))
                                .check(ArchitectureTest.CLASSES);
                    });
        });
    }

    public static final SliceRule NO_CYCLE_BETWEEN_MODULES = slices()
            .matching("com.blog.(*)..")
            .should().beFreeOfCycles()
            .because("模块间循环依赖会导致微服务拆分失败");

    public static final ArchRule COMMON_NO_BUSINESS_LOGIC = noClasses()
            .that().resideInAPackage("com.blog.common..")
            .should().beAnnotatedWith(org.springframework.stereotype.Service.class)
            .orShould().beAnnotatedWith(org.springframework.stereotype.Repository.class)
            .orShould().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .because("common 模块禁止包含业务逻辑");

    public static void check() {
        checkNoCrossModuleImplDependency();
        NO_CYCLE_BETWEEN_MODULES.check(ArchitectureTest.CLASSES);
        COMMON_NO_BUSINESS_LOGIC.check(ArchitectureTest.CLASSES);
    }
}