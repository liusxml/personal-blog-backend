package com.blog.comment.domain.processor;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 评论 Markdown 渲染处理器
 *
 * <p>
 * 将 Markdown 转换为 HTML
 * </p>
 *
 * @author liusxml
 * @since 1.4.0
 */
@Slf4j
@Component
public class CommentMarkdownProcessor implements ContentProcessor {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public CommentMarkdownProcessor() {
        MutableDataSet options = new MutableDataSet();
        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }

    @Override
    public ProcessResult process(ProcessContext context) {
        String content = context.getProcessedContent();

        if (StringUtils.isBlank(content)) {
            return ProcessResult.success();
        }

        try {
            Node document = parser.parse(content);
            String html = renderer.render(document);

            context.setRenderedHtml(html);
            log.debug("Markdown 渲染成功: {} -> {}", content.length(), html.length());

            return ProcessResult.success();
        } catch (Exception e) {
            log.error("Markdown 渲染失败", e);
            return ProcessResult.warning("Markdown 渲染失败: " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "Markdown渲染器";
    }

    @Override
    public int getOrder() {
        return 300; // 最后执行
    }
}
