package com.blog.common.exception;

import com.blog.common.enums.SystemErrorCode;

/**
 * 实体未找到异常类，用于处理资源查询失败的情况。
 * <p>
 * 该异常继承自 BusinessException，使用默认错误码 RESOURCE_NOT_FOUND (1004)。
 * 支持多种构造函数，便于生成详细的错误消息，例如指定实体名称和 ID。
 * 在 RESTful API 中，常映射到 HTTP 404 Not Found 响应。
 * 
 * @see BusinessException
 * @see SystemErrorCode#RESOURCE_NOT_FOUND
 * @since 1.0
 */
public class EntityNotFoundException extends BusinessException {

    /**
     * 默认构造函数，使用预定义的 RESOURCE_NOT_FOUND 错误码和消息。
     */
    public EntityNotFoundException() {
        super(SystemErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 带自定义消息的构造函数，允许覆盖默认错误消息。
     * 
     * @param message 自定义错误消息
     */
    public EntityNotFoundException(String message) {
        super(SystemErrorCode.RESOURCE_NOT_FOUND, message);
    }

    /**
     * 带实体名称和 ID 的构造函数，生成格式化的错误消息，如 "User with ID 123 not found"。
     * 
     * @param entityName 实体名称（如 "User" 或 "Article"）
     * @param id 实体 ID（支持任何 Serializable 类型）
     */
    public EntityNotFoundException(String entityName, Object id) {
        super(SystemErrorCode.RESOURCE_NOT_FOUND, entityName + " with ID " + id + " not found");
    }

    /**
     * 带实体名称、ID 和额外消息的构造函数，用于更复杂的场景。
     * 
     * @param entityName 实体名称
     * @param id 实体 ID
     * @param additionalMessage 额外描述信息
     */
    public EntityNotFoundException(String entityName, Object id, String additionalMessage) {
        super(SystemErrorCode.RESOURCE_NOT_FOUND, entityName + " with ID " + id + " not found: " + additionalMessage);
    }
}