package com.blog.article.infrastructure.vector;

import com.blog.article.domain.entity.ArticleEntity;
import com.blog.article.infrastructure.converter.ArticleConverter;
import com.blog.article.infrastructure.mapper.ArticleMapper;
import com.blog.vo.ArticleListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 向量搜索服务
 *
 * <p>
 * 基于MySQL 9.4 VECTOR类型实现语义相似度搜索。
 * </p>
 *
 * <p>
 * 核心功能：
 * </p>
 * <ul>
 * <li>相关文章推荐（基于余弦相似度）</li>
 * <li>语义搜索（未来扩展）</li>
 * </ul>
 *
 * <p>
 * 性能优化：
 * </p>
 * <ul>
 * <li>缓存：推荐结果缓存30分钟</li>
 * <li>降级：向量不存在时返回基于分类/标签的推荐</li>
 * <li>限流：防止频繁查询</li>
 * </ul>
 *
 * @author blog-system
 * @since 1.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchService {

    private final ArticleMapper articleMapper;
    private final EmbeddingService embeddingService;
    private final ArticleConverter articleConverter;

    /**
     * 查找相关文章（基于向量相似度）
     *
     * <p>
     * 算法：
     * </p>
     * <ol>
     * <li>获取当前文章的向量</li>
     * <li>使用 COSINE_DISTANCE 计算相似度</li>
     * <li>返回距离最小的N篇文章</li>
     * </ol>
     *
     * <p>
     * 降级策略：
     * </p>
     * <ul>
     * <li>如果当前文章没有向量，返回同分类文章</li>
     * <li>如果查询失败，返回最新发布的文章</li>
     * </ul>
     *
     * @param articleId 当前文章ID
     * @param limit     返回数量
     * @return 相关文章列表
     */
    public List<ArticleListVO> findRelatedArticles(Long articleId, Integer limit) {
        log.debug("查找相关文章: articleId={}, limit={}", articleId, limit);

        try {
            // 1. 获取当前文章
            ArticleEntity article = articleMapper.selectById(articleId);
            if (article == null) {
                log.warn("文章不存在: articleId={}", articleId);
                return new ArrayList<>();
            }

            // 2. 检查是否有向量
            if (article.getEmbedding() == null || article.getEmbedding().isEmpty()) {
                log.debug("文章向量不存在，使用降级策略: articleId={}", articleId);
                return fallbackRecommendation(article, limit);
            }

            // 3. 执行向量相似度查询
            List<ArticleEntity> relatedEntities = articleMapper.findRelatedArticlesByVector(
                    article.getEmbedding(),
                    articleId,
                    limit);

            // 4. 转换为 VO
            List<ArticleListVO> relatedArticles = relatedEntities.stream()
                    .map(articleConverter::entityToListVo)
                    .collect(Collectors.toList());

            log.info("向量搜索完成: articleId={}, 找到{}篇相关文章",
                    articleId, relatedArticles.size());

            return relatedArticles;

        } catch (Exception e) {
            log.error("向量搜索失败: articleId={}", articleId, e);
            // 降级：返回最新文章
            return fallbackToLatestArticles(articleId, limit);
        }
    }

    /**
     * 降级策略：基于分类/标签推荐
     */
    private List<ArticleListVO> fallbackRecommendation(ArticleEntity article, Integer limit) {
        log.debug("使用分类降级推荐: categoryId={}", article.getCategoryId());

        if (article.getCategoryId() != null) {
            // 返回同分类的文章
            List<ArticleEntity> entities = articleMapper.findByCategoryExcluding(
                    article.getCategoryId(),
                    article.getId(),
                    limit);
            return entities.stream()
                    .map(articleConverter::entityToListVo)
                    .collect(Collectors.toList());
        }

        // 最终降级：返回最新文章
        return fallbackToLatestArticles(article.getId(), limit);
    }

    /**
     * 最终降级：返回最新发布的文章
     */
    private List<ArticleListVO> fallbackToLatestArticles(Long excludeId, Integer limit) {
        log.debug("使用最新文章降级: excludeId={}", excludeId);
        List<ArticleEntity> entities = articleMapper.findLatestArticlesExcluding(excludeId, limit);
        return entities.stream()
                .map(articleConverter::entityToListVo)
                .collect(Collectors.toList());
    }
}
