package com.blog.article.domain.state;

import com.blog.article.domain.entity.ArticleEntity;
import com.blog.enums.ArticleStatus;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 草稿状态
 *
 * <p>
 * 文章创建后的初始状态，只有作者可见。
 * </p>
 *
 * <p>
 * 允许的操作：
 * </p>
 * <ul>
 * <li>✅ 发布 → PUBLISHED</li>
 * <li>✅ 删除</li>
 * <li>❌ 归档（草稿无法归档）</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.1.0
 */
@Slf4j
public class DraftState implements ArticleState {

    @Override
    public void publish(ArticleEntity article) {
        log.info("草稿 → 已发布: articleId={}", article.getId());

        // 设置发布状态和时间
        article.setStatus(ArticleStatus.PUBLISHED.getCode());
        article.setPublishTime(LocalDateTime.now());

        // 事件在 Service 层触发
    }

    @Override
    public void archive(ArticleEntity article) {
        throw new IllegalStateException("草稿状态的文章无法归档，请先发布");
    }

    @Override
    public void unarchive(ArticleEntity article) {
        throw new IllegalStateException("草稿状态的文章无法执行恢复操作");
    }

    @Override
    public boolean canDelete(ArticleEntity article) {
        // 草稿可以直接删除
        return true;
    }

    @Override
    public String getStateName() {
        return "草稿";
    }
}
