package com.blog.comment.domain.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 评论内容处理器责任链
 *
 * @author liusxml
 * @since 1.4.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentProcessorChain {

    private final List<ContentProcessor> processors;

    /**
     * 执行处理链
     *
     * @param content 原始内容
     * @return 处理上下文
     */
    public ProcessContext execute(String content) {
        ProcessContext context = new ProcessContext(content);

        // 按 order 排序
        processors.stream()
                .sorted(Comparator.comparingInt(ContentProcessor::getOrder))
                .forEach(processor -> {
                    if (!context.isPassed()) {
                        return; // 已失败，跳过后续处理器
                    }

                    log.debug("执行处理器: {}", processor.getName());
                    ProcessResult result = processor.process(context);

                    if (!result.isSuccess()) {
                        context.markAsFailed(result.getErrorMessage());
                        log.warn("处理器 {} 失败: {}", processor.getName(), result.getErrorMessage());
                    }

                    if (result.isShouldBreak()) {
                        log.info("处理器 {} 要求中断后续处理", processor.getName());
                    }
                });

        log.info("责任链处理完成: passed={}, processors={}", context.isPassed(), processors.size());
        return context;
    }
}
