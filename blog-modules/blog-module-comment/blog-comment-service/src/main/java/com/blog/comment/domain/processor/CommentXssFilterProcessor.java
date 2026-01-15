package com.blog.comment.domain.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 评论 XSS 过滤处理器
 *
 * <p>
 * 移除危险的 HTML 标签和脚本
 * </p>
 *
 * @author liusxml
 * @since 1.4.0
 */
@Slf4j
@Component
public class CommentXssFilterProcessor implements ContentProcessor {

    private static final String[] DANGEROUS_TAGS = {
            "<script", "</script>",
            "<iframe", "</iframe>",
            "javascript:",
            "onerror=",
            "onload=",
            "onclick=",
            "<object", "</object>",
            "<embed"
    };

    @Override
    public ProcessResult process(ProcessContext context) {
        String content = context.getProcessedContent();

        if (StringUtils.isBlank(content)) {
            return ProcessResult.success();
        }

        String filtered = filterXss(content);

        if (!filtered.equals(content)) {
            log.warn("XSS 内容已过滤: 原始长度={}, 过滤后长度={}", content.length(), filtered.length());
            context.updateContent(filtered);
            context.getMetadata().put("xss_filtered", true);
        }

        return ProcessResult.success();
    }

    /**
     * 过滤 XSS 内容
     */
    private String filterXss(String content) {
        String result = content;

        // 移除危险标签
        for (String dangerousTag : DANGEROUS_TAGS) {
            result = result.replaceAll("(?i)" + dangerousTag, "");
        }

        // 转义特殊字符（保留基本 Markdown）
        result = result.replace("<", "&lt;")
                .replace(">", "&gt;");

        return result;
    }

    @Override
    public String getName() {
        return "XSS过滤器";
    }

    @Override
    public int getOrder() {
        return 100; // 最先执行
    }
}
