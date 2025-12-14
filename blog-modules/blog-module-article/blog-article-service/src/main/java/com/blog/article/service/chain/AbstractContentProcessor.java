package com.blog.article.service.chain;

import lombok.extern.slf4j.Slf4j;

/**
 * 抽象内容处理器
 *
 * <p>
 * 提供责任链的基础实现。
 * </p>
 *
 * @author blog-system
 * @since 1.1.0
 */
@Slf4j
public abstract class AbstractContentProcessor implements ContentProcessor {

    protected ContentProcessor next;

    @Override
    public void setNext(ContentProcessor next) {
        this.next = next;
    }

    /**
     * 执行处理并传递给下一个处理器
     *
     * @param result 处理结果
     * @return 最终处理结果
     */
    @Override
    public ProcessResult process(ProcessResult result) {
        if (!result.isSuccess()) {
            log.warn("处理链中断: {}, 原因: {}", getName(), result.getErrorMessage());
            return result;
        }

        log.debug("执行处理器: {}", getName());

        // 执行当前处理器的逻辑
        ProcessResult processedResult = doProcess(result);

        // 如果有下一个处理器，继续传递
        if (next != null && processedResult.isSuccess()) {
            return next.process(processedResult);
        }

        return processedResult;
    }

    /**
     * 子类实现具体的处理逻辑
     *
     * @param result 当前结果
     * @return 处理后的结果
     */
    protected abstract ProcessResult doProcess(ProcessResult result);
}
