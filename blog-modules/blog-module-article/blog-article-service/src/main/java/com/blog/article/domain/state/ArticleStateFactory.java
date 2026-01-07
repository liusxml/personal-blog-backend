package com.blog.article.domain.state;

import com.blog.article.api.enums.ArticleStatus;
import com.blog.article.domain.entity.ArticleEntity;
import org.springframework.stereotype.Component;

/**
 * 文章状态工厂
 *
 * <p>
 * 负责根据状态码创建对应的状态对象。
 * </p>
 *
 * <p>
 * 使用单例模式缓存状态实例，避免重复创建。
 * </p>
 *
 * @author liusxml
 * @since 1.1.0
 */
@Component
public class ArticleStateFactory {

    // 状态实例（单例）
    private final DraftState draftState = new DraftState();
    private final PublishedState publishedState = new PublishedState();
    private final ArchivedState archivedState = new ArchivedState();

    /**
     * 根据状态码获取状态对象
     *
     * @param statusCode 状态码
     * @return 状态对象
     * @throws IllegalArgumentException 如果状态码无效
     */
    public ArticleState getState(Integer statusCode) {
        ArticleStatus status = ArticleStatus.of(statusCode);

        return switch (status) {
            case DRAFT -> draftState;
            case PUBLISHED -> publishedState;
            case ARCHIVED -> archivedState;
            default -> throw new IllegalArgumentException("未知的文章状态: " + statusCode);
        };
    }

    /**
     * 根据实体获取状态对象
     *
     * @param article 文章实体
     * @return 状态对象
     */
    public ArticleState getState(ArticleEntity article) {
        return getState(article.getStatus());
    }
}
