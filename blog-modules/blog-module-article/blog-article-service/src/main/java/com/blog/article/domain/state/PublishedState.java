package com.blog.article.domain.state;

import com.blog.article.domain.entity.ArticleEntity;
import com.blog.enums.ArticleStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * 已发布状态
 *
 * <p>
 * 文章公开可见，用户可以阅读和评论。
 * </p>
 *
 * <p>
 * 允许的操作：
 * </p>
 * <ul>
 * <li>✅ 归档 → ARCHIVED</li>
 * <li>✅ 删除（需二次确认）</li>
 * <li>❌ 发布（已经是发布状态）</li>
 * </ul>
 *
 * @author blog-system
 * @since 1.1.0
 */
@Slf4j
public class PublishedState implements ArticleState {

    @Override
    public void publish(ArticleEntity article) {
        log.warn("文章已经是发布状态: articleId={}", article.getId());
        // 幂等操作，不抛异常
    }

    @Override
    public void archive(ArticleEntity article) {
        log.info("已发布 → 已归档: articleId={}", article.getId());

        // 设置归档状态
        article.setStatus(ArticleStatus.ARCHIVED.getCode());
    }

    @Override
    public void unarchive(ArticleEntity article) {
        throw new IllegalStateException("已发布状态的文章无法执行恢复操作");
    }

    @Override
    public boolean canDelete(ArticleEntity article) {
        // 已发布的文章可以删除，但应该有警告
        log.warn("删除已发布的文章: articleId={}", article.getId());
        return true;
    }

    @Override
    public String getStateName() {
        return "已发布";
    }
}
