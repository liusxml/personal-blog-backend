package com.blog.article.service.chain.impl;

import com.blog.article.service.chain.AbstractContentProcessor;
import com.blog.article.service.chain.ProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 摘要提取处理器
 *
 * <p>
 * 从Markdown内容中自动提取摘要。
 * </p>
 *
 * <p>
 * 提取策略：
 * </p>
 * <ul>
 * <li>截取前200字符</li>
 * <li>移除Markdown标记</li>
 * <li>智能断句</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.1.0
 */
@Slf4j
@Component
public class SummaryExtractorProcessor extends AbstractContentProcessor {

    private static final int MAX_SUMMARY_LENGTH = 200;

    @Override
    protected ProcessResult doProcess(ProcessResult result) {
        String markdown = result.getMarkdown();

        if (markdown == null || markdown.isEmpty()) {
            result.setSummary("");
            return result;
        }

        String summary = extractSummary(markdown);
        result.setSummary(summary);

        log.debug("摘要提取完成: 长度={}", summary.length());

        return result;
    }

    /**
     * 提取摘要
     */
    private String extractSummary(String markdown) {
        // 移除Markdown标记
        String plainText = markdown
                .replaceAll("#{1,6}\\s+", "") // 移除标题标记
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1") // 移除粗体
                .replaceAll("\\*(.+?)\\*", "$1") // 移除斜体
                .replaceAll("`(.+?)`", "$1") // 移除代码
                .replaceAll("```[\\s\\S]*?```", "") // 移除代码块
                .replaceAll("!\\[.*?\\]\\(.*?\\)", "") // 移除图片
                .replaceAll("\\[(.+?)\\]\\(.*?\\)", "$1") // 替换链接为文本
                .replaceAll("\\s+", " ") // 合并空格
                .trim();

        // 截取前200字符
        if (plainText.length() <= MAX_SUMMARY_LENGTH) {
            return plainText;
        }

        // 智能断句（在句号、问号、感叹号处截断）
        String truncated = plainText.substring(0, MAX_SUMMARY_LENGTH);
        int lastSentence = Math.max(
                Math.max(truncated.lastIndexOf('。'), truncated.lastIndexOf('.')),
                Math.max(truncated.lastIndexOf('！'), truncated.lastIndexOf('!')));

        if (lastSentence > 100) { // 如果找到合适的断句点
            return truncated.substring(0, lastSentence + 1);
        }

        return truncated + "...";
    }
}
