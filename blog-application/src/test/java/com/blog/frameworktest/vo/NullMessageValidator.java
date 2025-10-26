// file: src/test/java/com/blog/frameworktest/validation/NullMessageValidator.java
package com.blog.frameworktest.vo;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NullMessageValidator implements ConstraintValidator<NullMessage, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 当字段的值为 "trigger-null-message" 时，我们手动构造一个 null 消息的校验失败信息
        if ("trigger-null-message".equals(value)) {
            // 1. 禁用默认的 violation
            context.disableDefaultConstraintViolation();
            // 2. 构造一个新的 violation，并把 messageTemplate 设置为 null
            context.buildConstraintViolationWithTemplate("")
                   .addConstraintViolation();
            return false; // 校验失败
        }
        return true; // 其他情况均校验成功
    }
}