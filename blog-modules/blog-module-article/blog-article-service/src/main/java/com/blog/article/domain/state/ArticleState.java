package com.blog.article.domain.state;

import com.blog.article.domain.entity.ArticleEntity;

/**
 * 文章状态接口（状态模式）
 *
 * <p>
 * 定义文章在不同状态下的行为。
 * </p>
 *
 * <p>
 * 状态模式优势：
 * </p>
 * <ul>
 * <li>消除大量 if-else 条件判断</li>
 * <li>每个状态的行为封装在独立类中</li>
 * <li>新增状态只需添加新类，符合开闭原则</li>
 * <li>状态转换规则清晰可控</li>
 * </ul>
 *
 * <p>
 * 状态流转图：
 * </p>
 * 
 * <pre>
 * DRAFT (草稿) → PUBLISHED (已发布) → ARCHIVED (已归档)
 *      ↓              ↓                    ↓
 *   DELETE         ARCHIVE              UNARCHIVE
 * </pre>
 *
 * @author blog-system
 * @since 1.1.0
 */
public interface ArticleState {

    /**
     * 发布文章
     *
     * @param article 文章实体
     * @throws IllegalStateException 如果当前状态不允许发布
     */
    void publish(ArticleEntity article);

    /**
     * 归档文章
     *
     * @param article 文章实体
     * @throws IllegalStateException 如果当前状态不允许归档
     */
    void archive(ArticleEntity article);

    /**
     * 恢复归档
     *
     * @param article 文章实体
     * @throws IllegalStateException 如果当前状态不允许恢复
     */
    void unarchive(ArticleEntity article);

    /**
     * 删除文章
     *
     * @param article 文章实体
     * @return true 如果允许删除
     */
    boolean canDelete(ArticleEntity article);

    /**
     * 获取状态名称
     *
     * @return 状态名称
     */
    String getStateName();
}
