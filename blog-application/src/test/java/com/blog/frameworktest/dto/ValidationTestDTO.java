// file: src/test/java/com/blog/frameworktest/model/ValidationTestDTO.java
package com.blog.frameworktest.dto;

import com.blog.frameworktest.vo.NullMessage;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ValidationTestDTO {
    /**
     * 一个标准的校验注解，它总会生成一个非null的错误消息
     */
    @NotBlank
    private String normalField;
    /**
     * 我们自定义的校验注解，用于模拟生成一个null的错误消息
     */
    @NullMessage
    private String fieldWithNullMessage;
}