// file: blog-common/src/main/java/com/blog/common/model/Result.java
package com.blog.common.model;

import com.blog.common.exception.ErrorCode;
import org.springframework.lang.Nullable;

/**
 * 全局统一响应结果类 (使用 JDK Record 实现)
 *
 * @param code    业务状态码 (0-成功, 其他-失败)
 * @param message 响应信息
 * @param data    响应数据
 * @param <T>     数据载荷的类型
 */
public record Result<T>(int code, String message, @Nullable T data) {

    private static final int SUCCESS_CODE = 0;
    private static final String SUCCESS_MESSAGE = "success";

    /**
     * 成功，不返回数据
     */
    public static <T> Result<T> success() {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    /**
     * 成功，并返回数据
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    /**
     * 失败，使用预定义的错误码
     */
    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }
    
    /**
     * 失败，使用预定义错误码和自定义消息
     * (覆盖默认消息，用于更具体的场景)
     */
    public static <T> Result<T> error(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }

    /**
     * 失败，自定义错误码和错误信息
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
