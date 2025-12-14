package com.blog.article.service.chain.impl;

import com.blog.article.service.chain.AbstractContentProcessor;
import com.blog.article.service.chain.ProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Markdown解析处理器
 *
 * <p>
 * 将Markdown内容转换为HTML。
 * </p>
 *
 * <p>
 * TODO: 集成 CommonMark 或 Flexmark 库进行专业解析。
 * </p>
 *
 * @author blog-system
 * @since 1.1.0
 */
@Slf4j
@Component
public class MarkdownParserProcessor extends AbstractContentProcessor {

    @Override
    protected ProcessResult doProcess(ProcessResult result) {
        String markdown = result.getMarkdown();

        if (markdown == null || markdown.isEmpty()) {
            result.setHtml("");
            return result;
        }

        // 简单的Markdown -> HTML转换（生产环境应使用专业库）
        String html = simpleMarkdownToHtml(markdown);

        result.setHtml(html);

        log.debug("Markdown解析完成: 输出HTML长度={}", html.length());

        return result;
    }

    /**
     * 简单的Markdown转HTML（仅演示用）
     * TODO: 替换为 CommonMark 或 Flexmark
     */
    private String simpleMarkdownToHtml(String markdown) {
        return markdown
                // 标题
                .replaceAll("(?m)^### (.+)$", "<h3>$1</h3>")
                .replaceAll("(?m)^## (.+)$", "<h2>$1</h2>")
                .replaceAll("(?m)^# (.+)$", "<h1>$1</h1>")
                // 粗体
                .replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>")
                // 斜体
                .replaceAll("\\*(.+?)\\*", "<em>$1</em>")
                // 代码块
                .replaceAll("```([\\s\\S]*?)```", "<pre><code>$1</code></pre>")
                // 行内代码
                .replaceAll("`(.+?)`", "<code>$1</code>")
                // 段落
                .replaceAll("(?m)^(?!<)(.+)$", "<p>$1</p>");
    }
}
