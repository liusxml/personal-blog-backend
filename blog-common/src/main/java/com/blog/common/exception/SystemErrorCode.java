package com.blog.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 系统模块错误码
 *
 * @author liusxml
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum SystemErrorCode implements ErrorCode {

    // 通用系统错误 (10xx)
    SYSTEM_ERROR(1001, "系统繁忙，请稍后再试"),
    INVALID_PARAMETER(1002, "参数错误"),
    VALIDATION_ERROR(1003, "参数校验失败"),
    RESOURCE_NOT_FOUND(1004, "资源未找到"),
    OPERATION_FAILED(1005, "操作失败"),
    NOT_FOUND(404, "Not Found"),

    // 参数错误 (400xx)
    PARAM_ERROR(40000, "参数错误"),

    // 用户相关错误 (400xx)
    USER_NOT_FOUND(40001, "用户不存在"),
    INVALID_CREDENTIALS(40002, "用户名或密码错误"),
    USER_DISABLED(40003, "用户已被禁用"),
    EMAIL_ALREADY_EXISTS(40004, "邮箱已被注册"),
    USERNAME_ALREADY_EXISTS(40005, "用户名已存在"),
    PASSWORD_NOT_MATCH(40006, "旧密码不正确"),

    // 角色相关错误 (400xx)
    ROLE_NOT_FOUND(40010, "角色不存在"),
    ROLE_KEY_CONFLICT(40011, "角色权限字符串已存在"),
    ROLE_IN_USE(40012, "角色正在使用中，无法删除"),

    // 认证相关错误 (401xx)
    INVALID_TOKEN(40101, "Token无效或已过期"),
    UNAUTHORIZED(40102, "未授权访问"),

    // 权限相关错误 (403xx)
    ACCESS_DENIED(40301, "无权访问该资源"),
    INSUFFICIENT_PERMISSIONS(40302, "权限不足");

    private final Integer code;
    private final String message;
}
