package com.blog.article.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.article.domain.entity.ArticleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文章 Mapper
 *
 * <p>
 * 继承 MyBatis-Plus BaseMapper，自动提供 CRUD 方法。
 * </p>
 *
 * <p>
 * 自定义查询方法可在对应的 mapper.xml 中编写，例如：
 * </p>
 * <ul>
 * <li>向量相似度查询（COSINE_DISTANCE）</li>
 * <li>复杂的多表关联查询</li>
 * </ul>
 *
 * @author blog-system
 * @since 1.1.0
 */
@Mapper
public interface ArticleMapper extends BaseMapper<ArticleEntity> {
        // MyBatis-Plus 已提供基础 CRUD 方法
        // 如需自定义 SQL，可在 resources/mapper/ArticleMapper.xml 中编写

        // BaseMapper 已提供基础 CRUD 方法
        // 可在此扩展自定义查询

        /**
         * 基于向量相似度查找相关文章
         *
         * <p>
         * 使用 MySQL 9.4 的 COSINE_DISTANCE 函数计算向量相似度。
         * </p>
         *
         * @param queryVector 查询向量（VECTOR格式字符串）
         * @param excludeId   排除的文章ID（当前文章）
         * @param limit       返回数量
         * @return 相关文章列表（按相似度降序）
         */
        @Select("""
                            SELECT id, title, summary, cover_image as coverImage,
                                   author_id as authorId, category_id as categoryId,
                                   publish_time as publishTime, is_top as isTop
                            FROM art_article
                            WHERE is_deleted = 0
                              AND status = 2
                              AND id != #{excludeId}
                              AND embedding IS NOT NULL
                            ORDER BY COSINE_DISTANCE(embedding, #{queryVector}) ASC
                            LIMIT #{limit}
                        """)
        List<ArticleEntity> findRelatedArticlesByVector(
                        @Param("queryVector") String queryVector,
                        @Param("excludeId") Long excludeId,
                        @Param("limit") Integer limit);

        /**
         * 查找同分类文章（降级策略1）
         *
         * @param categoryId 分类ID
         * @param excludeId  排除的文章ID
         * @param limit      返回数量
         * @return 同分类文章列表
         */
        @Select("""
                            SELECT id, title, summary, cover_image as coverImage,
                                   author_id as authorId, category_id as categoryId,
                                   publish_time as publishTime, is_top as isTop
                            FROM art_article
                            WHERE is_deleted = 0
                              AND status = 2
                              AND category_id = #{categoryId}
                              AND id != #{excludeId}
                            ORDER BY publish_time DESC
                            LIMIT #{limit}
                        """)
        List<ArticleEntity> findByCategoryExcluding(
                        @Param("categoryId") Long categoryId,
                        @Param("excludeId") Long excludeId,
                        @Param("limit") Integer limit);

        /**
         * 查找最新文章（降级策略2）
         *
         * @param excludeId 排除的文章ID
         * @param limit     返回数量
         * @return 最新文章列表
         */
        @Select("""
                            SELECT id, title, summary, cover_image as coverImage,
                                   author_id as authorId, category_id as categoryId,
                                   publish_time as publishTime, is_top as isTop
                            FROM art_article
                            WHERE is_deleted = 0
                              AND status = 2
                              AND id != #{excludeId}
                            ORDER BY publish_time DESC
                            LIMIT #{limit}
                        """)
        List<ArticleEntity> findLatestArticlesExcluding(
                        @Param("excludeId") Long excludeId,
                        @Param("limit") Integer limit);
}
