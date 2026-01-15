package com.blog.comment.domain.processor;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 处理结果
 *
 * @author liusxml
 * @since 1.4.0
 */
@Data
@AllArgsConstructor
public class ProcessResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误消息（仅失败时）
     */
    private String errorMessage;

    /**
     * 是否需要中断后续处理器
     */
    private boolean shouldBreak;

    public static ProcessResult success() {
        return new ProcessResult(true, null, false);
    }

    public static ProcessResult failure(String errorMessage) {
        return new ProcessResult(false, errorMessage, true);
    }

    public static ProcessResult warning(String message) {
        return new ProcessResult(true, message, false);
    }
}
