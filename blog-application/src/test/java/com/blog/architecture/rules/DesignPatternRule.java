package com.blog.architecture.rules;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.architecture.ArchitectureTest;
import com.blog.common.base.BaseConverter;
import com.blog.common.base.BaseServiceImpl;
import com.blog.common.base.IBaseService;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.GeneralCodingRules;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.regex.Pattern;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * 设计模式强制规则 - MapStruct、MyBatis-Plus、异常、构造器注入。
 */
public final class DesignPatternRule {

    /**
     * 正则：匹配 Service 层禁止手写的对象转换方法名（如 toDto/toVo/toEntity/fromDto）
     */
    private static final Pattern MANUAL_MAPPING_METHOD_NAME = Pattern
            .compile("^(toDto|toDTO|toVo|toVO|toEntity|fromDto|fromDTO)$");

    /*
     * --------------------------------------------------------------------- *
     * 1. 禁止在 Service 层手动映射（必须使用 MapStruct Converter）
     * ---------------------------------------------------------------------
     */
    public static final ArchRule NO_MANUAL_MAPPING_IN_SERVICE = classes()
            .that().resideInAPackage("com.blog..service..")
            .should(new ArchCondition<JavaClass>("not perform manual object mapping without MapStruct") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    for (JavaMethod method : javaClass.getMethods()) {
                        if (MANUAL_MAPPING_METHOD_NAME.matcher(method.getName()).matches()) {
                            String message = String.format(
                                    "Service method %s.%s() defines manual mapping method in %s; use MapStruct BaseConverter instead",
                                    method.getOwner().getSimpleName(), method.getName(),
                                    method.getSourceCodeLocation());
                            events.add(SimpleConditionEvent.violated(method, message));
                        }
                    }
                    for (JavaMethodCall call : javaClass.getMethodCallsFromSelf()) {
                        if (call.getTargetOwner().isEquivalentTo(BeanUtils.class)
                                && "copyProperties".equals(call.getName())) {
                            String message = String.format(
                                    "Service %s calls BeanUtils.copyProperties() in %s; use MapStruct BaseConverter instead",
                                    javaClass.getSimpleName(), call.getSourceCodeLocation());
                            events.add(SimpleConditionEvent.violated(call, message));
                        }
                    }
                }
            })
            .because("禁止在 Service 中手动映射或调用 BeanUtils.copyProperties，应使用 MapStruct Converter");

    /*
     * --------------------------------------------------------------------- *
     * 2. 所有持久层 Mapper 接口必须使用 MyBatis @Mapper 注解并继承 BaseMapper
     * ---------------------------------------------------------------------
     */
    public static final ArchRule MAPPER_MUST_BE_ANNOTATED = classes()
            .that().resideInAPackage("com.blog..mapper..")
            .and().areInterfaces()
            .should().beAnnotatedWith(Mapper.class)
            .andShould().beAssignableTo(BaseMapper.class)
            .because("所有持久层 Mapper 接口必须使用 MyBatis @Mapper 注解并继承 BaseMapper");

    /*
     * --------------------------------------------------------------------- *
     * 3. 所有对象转换器接口必须继承 BaseConverter
     * ---------------------------------------------------------------------
     */
    public static final ArchRule CONVERTER_SHOULD_EXTEND_BASE = classes()
            .that().resideInAPackage("com.blog..converter..")
            .and().areInterfaces()
            .should().beAssignableTo(BaseConverter.class)
            .because("所有 MapStruct 转换器接口必须继承 BaseConverter 以统一转换规范");

    /*
     * --------------------------------------------------------------------- *
     * 4. 禁止使用泛型异常（Exception / RuntimeException）
     * ---------------------------------------------------------------------
     */
    public static final ArchRule NO_GENERIC_EXCEPTION = GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    /*
     * --------------------------------------------------------------------- *
     * 5. 禁止字段注入（@Autowired on fields）
     * ---------------------------------------------------------------------
     */
    public static final ArchRule NO_FIELD_INJECTION = noFields()
            .should().beAnnotatedWith(Autowired.class)
            .because("禁止使用字段注入，应使用构造器注入（@RequiredArgsConstructor）或可选依赖 Setter 注入");

    /*
     * --------------------------------------------------------------------- *
     * 6. 实现 IBaseService 的标准 CRUD Service 实现类必须继承 BaseServiceImpl
     * ---------------------------------------------------------------------
     */
    public static final ArchRule SERVICE_IMPL_SHOULD_EXTEND_BASE = classes()
            .that().resideInAPackage("..service.impl..")
            .and().areNotInterfaces()
            .and().areAssignableTo(IBaseService.class)
            .should().beAssignableTo(BaseServiceImpl.class)
            .because("实现 IBaseService 的标准 Service 实现类必须继承 BaseServiceImpl 以复用 CRUD");

    /**
     * 私有构造器，防止实例化（工具类）
     */
    private DesignPatternRule() {
    }

    /*
     * --------------------------------------------------------------------- *
     * 7. 统一执行入口（模板方法）
     * ---------------------------------------------------------------------
     */
    public static void check() {
        NO_MANUAL_MAPPING_IN_SERVICE.check(ArchitectureTest.CLASSES);
        MAPPER_MUST_BE_ANNOTATED.check(ArchitectureTest.CLASSES);
        CONVERTER_SHOULD_EXTEND_BASE.check(ArchitectureTest.CLASSES);
        NO_GENERIC_EXCEPTION.check(ArchitectureTest.CLASSES);
        NO_FIELD_INJECTION.check(ArchitectureTest.CLASSES);
        SERVICE_IMPL_SHOULD_EXTEND_BASE.check(ArchitectureTest.CLASSES);

        // ---------- 通用编码规则（ArchUnit 官方库） ----------
        GeneralCodingRules.NO_CLASSES_SHOULD_USE_JODATIME.check(ArchitectureTest.CLASSES);
        GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.check(ArchitectureTest.CLASSES);
        GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(ArchitectureTest.CLASSES);
    }
}