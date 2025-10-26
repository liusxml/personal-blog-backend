// file: src/test/java/com/blog/frameworktest/validation/NullMessage.java
package com.blog.frameworktest.vo;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NullMessageValidator.class)
@Documented
public @interface NullMessage {
    String message() default "this message will be ignored";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}