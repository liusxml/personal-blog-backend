package com.blog.article.domain.state;

import com.blog.article.domain.entity.ArticleEntity;
import com.blog.enums.ArticleStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * 已归档状态
 *
 * <p>
 * 文章不再显示在列表中，但可通过直接链接访问。
 * </p>
 *
 * <p>
 * 允许的操作：
 * </p>
 * <ul>
 * <li>✅ 恢复 → PUBLISHED</li>
 * <li>✅ 删除</li>
 * <li>❌ 发布（需先恢复）</li>
 * <li>❌ 归档（已经是归档状态）</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.1.0
 */
@Slf4j
public class ArchivedState implements ArticleState {

    @Override
    public void publish(ArticleEntity article) {
        throw new IllegalStateException("已归档的文章无法直接发布，请先恢复归档");
    }

    @Override
    public void archive(ArticleEntity article) {
        log.warn("文章已经是归档状态: articleId={}", article.getId());
        // 幂等操作，不抛异常
    }

    @Override
    public void unarchive(ArticleEntity article) {
        log.info("已归档 → 已发布: articleId={}", article.getId());

        // 恢复为已发布状态
        article.setStatus(ArticleStatus.PUBLISHED.getCode());
    }

    @Override
    public boolean canDelete(ArticleEntity article) {
        // 已归档的文章可以删除
        return true;
    }

    @Override
    public String getStateName() {
        return "已归档";
    }
}
