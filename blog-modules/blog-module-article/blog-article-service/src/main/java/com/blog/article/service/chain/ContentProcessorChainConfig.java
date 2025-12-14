package com.blog.article.service.chain;

import com.blog.article.service.chain.impl.MarkdownParserProcessor;
import com.blog.article.service.chain.impl.SummaryExtractorProcessor;
import com.blog.article.service.chain.impl.TocGeneratorProcessor;
import com.blog.article.service.chain.impl.XssFilterProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 内容处理链配置
 *
 * <p>
 * 构建完整的内容处理链。
 * </p>
 *
 * <p>
 * 处理顺序：
 * </p>
 * 
 * <pre>
 * 1. XssFilter        - XSS过滤
 * 2. MarkdownParser   - Markdown转HTML
 * 3. TocGenerator     - 提取目录
 * 4. SummaryExtractor - 提取摘要
 * </pre>
 *
 * @author blog-system
 * @since 1.1.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ContentProcessorChainConfig {

    private final XssFilterProcessor xssFilterProcessor;
    private final MarkdownParserProcessor markdownParserProcessor;
    private final TocGeneratorProcessor tocGeneratorProcessor;
    private final SummaryExtractorProcessor summaryExtractorProcessor;

    /**
     * 构建处理链
     *
     * @return 链头（XSS过滤器）
     */
    @Bean
    public ContentProcessor contentProcessorChain() {
        // 构建责任链
        xssFilterProcessor.setNext(markdownParserProcessor);
        markdownParserProcessor.setNext(tocGeneratorProcessor);
        tocGeneratorProcessor.setNext(summaryExtractorProcessor);

        log.info("内容处理链初始化完成: XSS → Markdown → TOC → Summary");

        // 返回链头
        return xssFilterProcessor;
    }
}
