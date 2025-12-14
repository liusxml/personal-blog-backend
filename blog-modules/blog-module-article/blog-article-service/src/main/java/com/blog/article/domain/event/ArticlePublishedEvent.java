package com.blog.article.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 文章发布事件
 *
 * <p>
 * 当文章从草稿状态变为已发布状态时触发。
 * </p>
 *
 * <p>
 * 观察者模式优势：
 * </p>
 * <ul>
 * <li>解耦：核心业务逻辑与副作用分离</li>
 * <li>异步：监听器可以异步执行，提升响应速度</li>
 * <li>扩展：新增监听器不影响现有代码</li>
 * <li>灵活：可以动态启用/禁用监听器</li>
 * </ul>
 *
 * <p>
 * 监听器处理的副作用：
 * </p>
 * <ul>
 * <li>清理缓存</li>
 * <li>初始化统计数据</li>
 * <li>生成文章向量（异步）</li>
 * <li>发送通知给订阅者</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.1.0
 */
@Getter
public class ArticlePublishedEvent extends ApplicationEvent {

    /**
     * 文章ID
     */
    private final Long articleId;

    /**
     * 作者ID
     */
    private final Long authorId;

    /**
     * 文章标题
     */
    private final String title;

    /**
     * 发布时间（毫秒时间戳）
     */
    private final Long publishTime;

    public ArticlePublishedEvent(Object source, Long articleId, Long authorId, String title) {
        super(source);
        this.articleId = articleId;
        this.authorId = authorId;
        this.title = title;
        this.publishTime = System.currentTimeMillis();
    }
}
