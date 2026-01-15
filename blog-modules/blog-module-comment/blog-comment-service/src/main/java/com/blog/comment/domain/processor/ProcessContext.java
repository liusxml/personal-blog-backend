package com.blog.comment.domain.processor;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 处理上下文（携带内容和元数据）
 *
 * @author liusxml
 * @since 1.4.0
 */
@Data
public class ProcessContext {

    /**
     * 原始内容
     */
    private String originalContent;

    /**
     * 处理后的内容
     */
    private String processedContent;

    /**
     * 渲染后的 HTML（Markdown→HTML）
     */
    private String renderedHtml;

    /**
     * 是否通过所有检查
     */
    private boolean passed = true;

    /**
     * 失败原因
     */
    private String failureReason;

    /**
     * 额外元数据（扩展字段）
     */
    private Map<String, Object> metadata = new HashMap<>();

    public ProcessContext(String content) {
        this.originalContent = content;
        this.processedContent = content;
    }

    /**
     * 更新处理后的内容
     */
    public void updateContent(String newContent) {
        this.processedContent = newContent;
    }

    /**
     * 标记为失败
     */
    public void markAsFailed(String reason) {
        this.passed = false;
        this.failureReason = reason;
    }
}
