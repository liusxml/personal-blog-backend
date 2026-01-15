package com.blog.common.exception;

import lombok.Getter;

/**
 * 操作失败异常类，用于表示一个预期的操作未能成功执行。
 * <p>
 * 该异常通常在数据库 CUD (Create/Update/Delete) 操作返回 false 或失败时抛出。
 * 它继承自 BusinessException，并建议使用一个特定的错误码，例如 OPERATION_FAILED (1005)。
 * 相比于通用的 RuntimeException，它能提供更明确的业务语义。
 *
 * @see BusinessException
 * @see SystemErrorCode#OPERATION_FAILED
 * @since 1.0
 */
@Getter
public class OperationFailedException extends BusinessException {

    /**
     * 携带的额外数据，可用于日志记录，但不一定返回给前端。
     */
    private final transient Object payload;

    /**
     * 默认构造函数，使用预定义的 OPERATION_FAILED 错误码和消息。
     * <p>
     * <b>注意:</b> 您需要在您的 SystemErrorCode 枚举中定义 OPERATION_FAILED，例如：
     *
     * <pre>{@code
     * OPERATION_FAILED(1005, "操作失败")
     * }</pre>
     */
    public OperationFailedException() {
        super(SystemErrorCode.OPERATION_FAILED);
        this.payload = null;
    }

    /**
     * 带自定义消息的构造函数。
     *
     * @param message 自定义错误消息，将覆盖默认消息。
     */
    public OperationFailedException(String message) {
        super(SystemErrorCode.OPERATION_FAILED, message);
        this.payload = null;
    }

    /**
     * 带自定义消息和额外载荷的构造函数。
     * 载荷（payload）可以是任何对象，通常是导致操作失败的数据实体，用于增强日志信息。
     *
     * @param message 自定义错误消息。
     * @param payload 导致操作失败的相关数据对象，用于日志记录。
     */
    public OperationFailedException(String message, Object payload) {
        super(SystemErrorCode.OPERATION_FAILED, message);
        this.payload = payload;
    }

    /**
     * 仅带额外载荷的构造函数，使用默认错误消息。
     *
     * @param payload 导致操作失败的相关数据对象。
     */
    public OperationFailedException(Object payload) {
        super(SystemErrorCode.OPERATION_FAILED);
        this.payload = payload;
    }
}
