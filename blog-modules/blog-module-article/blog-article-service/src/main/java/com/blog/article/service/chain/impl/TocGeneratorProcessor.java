package com.blog.article.service.chain.impl;

import com.blog.article.service.chain.AbstractContentProcessor;
import com.blog.article.service.chain.ProcessResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 目录生成处理器
 *
 * <p>
 * 从Markdown内容中提取标题，生成目录结构（TOC）。
 * </p>
 *
 * @author liusxml
 * @since 1.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TocGeneratorProcessor extends AbstractContentProcessor {

    private static final Pattern HEADING_PATTERN = Pattern.compile("(?m)^(#{1,6})\\s+(.+)$");
    private final ObjectMapper objectMapper;

    @Override
    protected ProcessResult doProcess(ProcessResult result) {
        String markdown = result.getMarkdown();

        if (markdown == null || markdown.isEmpty()) {
            result.setTocJson("[]");
            return result;
        }

        List<TocItem> toc = extractToc(markdown);

        try {
            String tocJson = objectMapper.writeValueAsString(toc);
            result.setTocJson(tocJson);
            log.debug("目录生成完成: 共{}个标题", toc.size());
        } catch (JsonProcessingException e) {
            log.error("TOC序列化失败", e);
            result.setTocJson("[]");
        }

        return result;
    }

    /**
     * 提取目录结构
     */
    private List<TocItem> extractToc(String markdown) {
        List<TocItem> toc = new ArrayList<>();
        Matcher matcher = HEADING_PATTERN.matcher(markdown);

        int index = 0;
        while (matcher.find()) {
            int level = matcher.group(1).length();
            String title = matcher.group(2).trim();

            toc.add(new TocItem(index++, level, title));
        }

        return toc;
    }

    /**
     * TOC项
     */
    private record TocItem(int index, int level, String title) {
    }
}
