package com.blog.comment.domain.processor;

/**
 * 内容处理器接口
 *
 * @author liusxml
 * @since 1.4.0
 */
public interface ContentProcessor {

    /**
     * 处理内容
     *
     * @param context 处理上下文
     * @return 处理结果
     */
    ProcessResult process(ProcessContext context);

    /**
     * 获取处理器名称
     */
    String getName();

    /**
     * 获取处理器执行顺序（越小越先执行）
     */
    int getOrder();
}
