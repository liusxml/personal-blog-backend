package com.blog.common.enums;

import com.blog.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum SystemErrorCode implements ErrorCode {
    // ========== 系统级别错误码 (1000-1999) ==========
    SYSTEM_ERROR(1001, "系统繁忙，请稍后再试"),
    INVALID_PARAMETER(1002, "参数错误"),
    VALIDATION_ERROR(1003, "参数校验失败"),
    RESOURCE_NOT_FOUND(1004, "资源未找到"),
    OPERATION_FAILED(1005, "操作失败"),

    // ========== 用户业务错误码 (2000-2999) ==========
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_PASSWORD_ERROR(2002, "用户名或密码错误"),

    // ========== 文章业务错误码 (3000-3999) ==========
    ARTICLE_NOT_FOUND(3001, "文章不存在");

    private final Integer code;
    private final String message;

    SystemErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}

