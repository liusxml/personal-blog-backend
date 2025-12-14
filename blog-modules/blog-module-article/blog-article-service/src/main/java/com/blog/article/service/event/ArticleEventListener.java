package com.blog.article.service.event;

import com.blog.article.domain.event.ArticlePublishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 文章事件监听器
 *
 * <p>
 * 监听文章相关事件并执行副作用操作。
 * </p>
 *
 * <p>
 * 设计要点：
 * </p>
 * <ul>
 * <li>使用 @Async 异步执行，不阻塞主业务流程</li>
 * <li>每个副作用独立处理，互不影响</li>
 * <li>异常不会影响核心业务（事件发布成功即可）</li>
 * </ul>
 *
 * @author blog-system
 * @since 1.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleEventListener {

    private final com.blog.article.infrastructure.vector.EmbeddingService embeddingService;
    // TODO: 注入所需的Service（缓存、统计等 Day 5实现）
    // private final ArticleStatsService statsService;
    // private final CacheManager cacheManager;

    /**
     * 处理文章发布事件
     *
     * <p>
     * 副作用列表：
     * </p>
     * <ol>
     * <li>清理相关缓存</li>
     * <li>初始化统计数据</li>
     * <li>异步生成文章向量</li>
     * <li>发送通知</li>
     * </ol>
     *
     * @param event 发布事件
     */
    @Async
    @EventListener
    public void handleArticlePublished(ArticlePublishedEvent event) {
        log.info("处理文章发布事件: articleId={}, title={}", event.getArticleId(), event.getTitle());

        try {
            // 1. 清理缓存
            clearCache(event);

            // 2. 初始化统计数据
            initializeStats(event);

            // 3. 生成向量（异步，不阻塞）
            generateEmbedding(event);

            // 4. 发送通知
            sendNotification(event);

            log.info("文章发布事件处理完成: articleId={}", event.getArticleId());
        } catch (Exception e) {
            // 异常不应影响核心业务
            log.error("文章发布事件处理失败: articleId={}", event.getArticleId(), e);
        }
    }

    /**
     * 清理缓存
     */
    private void clearCache(ArticlePublishedEvent event) {
        log.debug("清理文章缓存: articleId={}", event.getArticleId());
        // TODO: cacheManager.evict("article::" + event.getArticleId());
        // TODO: cacheManager.evict("article::list");
    }

    /**
     * 初始化统计数据
     */
    private void initializeStats(ArticlePublishedEvent event) {
        log.debug("初始化文章统计: articleId={}", event.getArticleId());
        // TODO: statsService.initStats(event.getArticleId());
    }

    /**
     * 生成文章向量
     */
    private void generateEmbedding(ArticlePublishedEvent event) {
        log.debug("开始生成文章向量: articleId={}", event.getArticleId());
        // 异步生成并保存向量
        embeddingService.generateAndSaveAsync(event.getArticleId());
    }

    /**
     * 发送通知
     */
    private void sendNotification(ArticlePublishedEvent event) {
        log.debug("发送文章发布通知: authorId={}", event.getAuthorId());
        // TODO: notificationService.notifySubscribers(event.getAuthorId(),
        // event.getTitle());
    }
}
