package com.blog.architecture.rules;

import com.blog.architecture.ArchitectureTest;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.GeneralCodingRules;
import org.mapstruct.Mapper;

import java.util.regex.Pattern;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 设计模式强制规则 - MapStruct、异常、Javadoc。
 */
public final class DesignPatternRule {

    /** 私有构造器，防止实例化（工具类） */
    private DesignPatternRule() {}

    /** 正则：匹配 get/set/toDTO/fromDTO 等映射方法名 */
    private static final Pattern MAPPING_METHOD_NAME = Pattern.compile(".*(get|set|toDTO|fromDTO).*");

    /* --------------------------------------------------------------------- *
     * 1. 禁止在 Service 层手动映射（必须使用 @Mapper）
     * --------------------------------------------------------------------- */
    public static final ArchRule NO_MANUAL_MAPPING_IN_SERVICE = noClasses()
            .that().resideInAPackage("com.blog..service..")
            // 修复 1：定义一个匿名 ArchCondition<JavaClass>，在内部检查每个方法
            .should(new ArchCondition<JavaClass>("perform manual mapping without @Mapper") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    for (JavaMethod method : javaClass.getMethods()) {
                        boolean nameMatches = MAPPING_METHOD_NAME.matcher(method.getName()).matches();
                        boolean ownerNotMapper = !method.getOwner().isAnnotatedWith(Mapper.class);
                        if (nameMatches && ownerNotMapper) {
                            String message = String.format(
                                    "Service method %s.%s() performs manual mapping but class is not annotated with @Mapper in %s",
                                    method.getOwner().getSimpleName(), method.getName(), method.getSourceCodeLocation());
                            events.add(SimpleConditionEvent.violated(method, message));
                        }
                    }
                }
            })
            .because("禁止在 Service 中手动映射，应使用 MapStruct");

    /* --------------------------------------------------------------------- *
     * 2. 所有 Mapper 接口必须使用 @Mapper 注解
     * --------------------------------------------------------------------- */
    public static final ArchRule MAPPER_MUST_BE_ANNOTATED = classes()
            .that().resideInAPackage("com.blog..mapper..")
            .and().areInterfaces()
            .should().beAnnotatedWith(Mapper.class)
            .because("所有 Mapper 必须使用 @Mapper 注解");

    /* --------------------------------------------------------------------- *
     * 3. 禁止使用泛型异常（Exception / RuntimeException）
     * --------------------------------------------------------------------- */
//    public static final ArchRule NO_GENERIC_EXCEPTION = noClasses()
//            .should(ArchConditions.declareThrowableOfType(Exception.class)
//                    .or(ArchConditions.declareThrowableOfType(RuntimeException.class))
//                    .or(ArchConditions.callConstructor(Exception.class))
//                    .or(ArchConditions.callConstructor(RuntimeException.class)))
//            .because("禁止使用泛型异常，应定义业务异常（如 BusinessException）");
    public static final ArchRule NO_GENERIC_EXCEPTION = GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    /* --------------------------------------------------------------------- *
     * 4. 统一执行入口（模板方法）
     * --------------------------------------------------------------------- */
    public static void check() {
        NO_MANUAL_MAPPING_IN_SERVICE.check(ArchitectureTest.CLASSES);
        MAPPER_MUST_BE_ANNOTATED.check(ArchitectureTest.CLASSES);
        NO_GENERIC_EXCEPTION.check(ArchitectureTest.CLASSES);

        // ---------- 通用编码规则（ArchUnit 官方库） ----------
        GeneralCodingRules.NO_CLASSES_SHOULD_USE_JODATIME.check(ArchitectureTest.CLASSES);
        GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.check(ArchitectureTest.CLASSES);
        GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(ArchitectureTest.CLASSES);
    }
}