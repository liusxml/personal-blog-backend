package com.blog.article.service.chain;

import lombok.Data;

/**
 * 内容处理结果
 *
 * <p>
 * 封装责任链处理后的结果。
 * </p>
 *
 * @author liusxml
 * @since 1.1.0
 */
@Data
public class ProcessResult {

    /**
     * 处理后的HTML内容
     */
    private String html;

    /**
     * 自动提取的摘要
     */
    private String summary;

    /**
     * 目录结构（JSON格式）
     */
    private String tocJson;

    /**
     * 原始Markdown内容
     */
    private String markdown;

    /**
     * 是否通过所有处理器
     */
    private boolean success = true;

    /**
     * 错误信息（如果有）
     */
    private String errorMessage;

    public static ProcessResult success() {
        ProcessResult result = new ProcessResult();
        result.setSuccess(true);
        return result;
    }

    public static ProcessResult fail(String errorMessage) {
        ProcessResult result = new ProcessResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
}
