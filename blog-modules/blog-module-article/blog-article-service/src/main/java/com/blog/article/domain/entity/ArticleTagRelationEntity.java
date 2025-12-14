package com.blog.article.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 文章-标签关联实体
 *
 * <p>
 * 映射数据库表 {@code art_article_tag}。
 * </p>
 *
 * <p>
 * 说明：
 * </p>
 * <ul>
 * <li>多对多关系中间表</li>
 * <li>不包含公共字段（轻量级关联表）</li>
 * <li>通过唯一索引保证数据一致性</li>
 * </ul>
 *
 * @author blog-system
 * @since 1.1.0
 */
@Data
@TableName("art_article_tag")
public class ArticleTagRelationEntity {

    /**
     * 主键ID（雪花算法自动生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文章ID（外键 art_article.id）
     */
    private Long articleId;

    /**
     * 标签ID（外键 art_tag.id）
     */
    private Long tagId;
}
