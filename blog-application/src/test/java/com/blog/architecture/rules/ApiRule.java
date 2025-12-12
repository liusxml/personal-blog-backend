package com.blog.architecture.rules;

import com.blog.architecture.ArchitectureTest;
import com.blog.common.model.Result;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * API 规范规则 - 强制统一的 Controller 响应格式和安全约束
 *
 * @author ArchUnit Team
 * @since 1.0
 */
public final class ApiRule {

    /** 私有构造器，防止实例化（工具类） */
    private ApiRule() {
    }

    /*
     * --------------------------------------------------------------------- *
     * 1. Controller 方法必须返回 Result<T>（特殊场景豁免）
     * ---------------------------------------------------------------------
     */
    public static final ArchRule CONTROLLER_MUST_RETURN_RESULT = methods()
            .that().areDeclaredInClassesThat()
            .areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .and().arePublic()
            .and().areNotAnnotatedWith(ExceptionHandler.class) // 豁免异常处理器
            .and().doNotHaveName("download.*") // 豁免下载方法
            .and().doNotHaveName("upload.*") // 豁免上传方法
            .should(new ArchCondition<JavaMethod>("return Result<T> or whitelisted types") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                    JavaClass returnType = method.getRawReturnType();

                    // 豁免特殊返回类型白名单
                    if (returnType.isAssignableTo(ResponseEntity.class) ||
                            returnType.isAssignableTo(SseEmitter.class) ||
                            returnType.getName().equals("void") ||
                            returnType.getName().equals("java.lang.Void")) {
                        return; // 允许这些特殊类型
                    }

                    // 检查是否返回 Result
                    if (!returnType.isAssignableTo(Result.class)) {
                        String message = String.format(
                                "Controller method %s.%s() returns '%s' instead of Result<T>. " +
                                        "SOLUTION: Change return type to Result<%s> and wrap response with Result.success().",
                                method.getOwner().getSimpleName(),
                                method.getName(),
                                returnType.getSimpleName(),
                                returnType.getSimpleName());
                        events.add(SimpleConditionEvent.violated(method, message));
                    }
                }
            })
            .because("所有 Controller 方法必须返回统一的 Result<T>，特殊场景除外（文件下载、SSE等）");

    /*
     * --------------------------------------------------------------------- *
     * 2. Controller 禁止直接返回 Entity
     * ---------------------------------------------------------------------
     */
    public static final ArchRule CONTROLLER_NO_ENTITY_IN_RESPONSE = methods()
            .that().areDeclaredInClassesThat()
            .areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .and().arePublic()
            .should(new ArchCondition<JavaMethod>("not return Entity directly") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                    JavaClass returnType = method.getRawReturnType();
                    String returnTypeName = returnType.getName();

                    // 检查是否直接返回 Entity（包路径包含 .entity. 或类名以 Entity 结尾）
                    boolean isEntity = returnTypeName.contains(".entity.") ||
                            (returnTypeName.endsWith("Entity")
                                    && !returnTypeName.equals("org.springframework.http.HttpEntity"));

                    if (isEntity) {
                        String message = String.format(
                                "Controller method %s.%s() returns Entity type '%s'. " +
                                        "SOLUTION: Create a DTO in *-api module and use MapStruct to convert Entity to DTO.",
                                method.getOwner().getSimpleName(),
                                method.getName(),
                                returnType.getSimpleName());
                        events.add(SimpleConditionEvent.violated(method, message));
                    }
                }
            })
            .because("Controller 禁止直接返回 Entity，必须映射为 DTO 以避免暴露内部结构");

    /*
     * --------------------------------------------------------------------- *
     * 3. 统一执行入口（模板方法）
     * ---------------------------------------------------------------------
     */
    public static void check() {
        CONTROLLER_MUST_RETURN_RESULT.check(ArchitectureTest.CLASSES);
        CONTROLLER_NO_ENTITY_IN_RESPONSE.check(ArchitectureTest.CLASSES);
    }
}
