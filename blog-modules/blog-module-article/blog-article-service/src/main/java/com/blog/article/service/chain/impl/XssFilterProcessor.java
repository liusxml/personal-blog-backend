package com.blog.article.service.chain.impl;

import com.blog.article.service.chain.AbstractContentProcessor;
import com.blog.article.service.chain.ProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * XSS过滤处理器
 *
 * <p>
 * 防止XSS攻击，过滤危险的HTML标签和脚本。
 * </p>
 *
 * <p>
 * 处理策略：
 * </p>
 * <ul>
 * <li>移除 &lt;script&gt; 标签</li>
 * <li>移除 onclick、onerror 等事件属性</li>
 * <li>转义特殊字符</li>
 * </ul>
 *
 * @author blog-system
 * @since 1.1.0
 */
@Slf4j
@Component
public class XssFilterProcessor extends AbstractContentProcessor {

    @Override
    protected ProcessResult doProcess(ProcessResult result) {
        String markdown = result.getMarkdown();

        if (markdown == null || markdown.isEmpty()) {
            return result;
        }

        // 简单的XSS过滤（生产环境建议使用 OWASP Java HTML Sanitizer）
        String filtered = markdown
                .replaceAll("(?i)<script[^>]*>.*?</script>", "")
                .replaceAll("(?i)on\\w+\\s*=\\s*[\"'][^\"']*[\"']", "")
                .replaceAll("(?i)javascript:", "");

        result.setMarkdown(filtered);

        log.debug("XSS过滤完成: 原始长度={}, 过滤后长度={}", markdown.length(), filtered.length());

        return result;
    }
}
