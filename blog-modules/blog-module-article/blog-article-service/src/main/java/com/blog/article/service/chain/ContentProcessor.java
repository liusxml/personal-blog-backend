package com.blog.article.service.chain;

/**
 * 内容处理器接口（责任链模式）
 *
 * <p>
 * 定义统一的处理器接口，每个处理器负责一种特定的内容处理任务。
 * </p>
 *
 * <p>
 * 责任链优势：
 * </p>
 * <ul>
 * <li>职责单一：每个处理器只做一件事</li>
 * <li>灵活组合：可以动态调整处理顺序</li>
 * <li>易于扩展：新增处理器不影响现有代码</li>
 * <li>可配置：可以根据需求启用/禁用特定处理器</li>
 * </ul>
 *
 * <p>
 * 处理链示例：
 * </p>
 *
 * <pre>
 * XssFilter → MarkdownParser → TocGenerator → SummaryExtractor
 * </pre>
 *
 * @author liusxml
 * @since 1.1.0
 */
public interface ContentProcessor {

    /**
     * 处理内容
     *
     * @param result 当前处理结果（会被逐步填充）
     * @return 处理后的结果
     */
    ProcessResult process(ProcessResult result);

    /**
     * 设置下一个处理器
     *
     * @param next 下一个处理器
     */
    void setNext(ContentProcessor next);

    /**
     * 获取处理器名称（用于日志）
     *
     * @return 处理器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }
}
