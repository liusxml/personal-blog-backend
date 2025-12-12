// file: src/test/java/com/blog/frameworktest/model/ValidationTestDTO.java
package com.blog.frameworktest.dto;

import com.blog.common.base.Identifiable;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class ValidationTestDTO implements Identifiable<Long>, Serializable {
    private Long id;

    /**
     * 一个标准的校验注解，它总会生成一个非null的错误消息
     */
    @NotBlank
    private String normalField;
    /**
     * 用于测试校验的字段
     */
    private String fieldWithNullMessage;
}