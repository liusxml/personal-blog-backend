package com.blog.article.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章统计实体
 *
 * <p>
 * 映射数据库表 {@code art_article_stats}。
 * </p>
 *
 * <p>
 * 设计原因：
 * </p>
 * <ul>
 * <li>将高频更新的统计数据独立拆分</li>
 * <li>避免主表行锁竞争，提升并发性能</li>
 * <li>支持独立的缓存策略</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.1.0
 */
@Data
@TableName("art_article_stats")
public class ArticleStatsEntity {

    // ========== 主键 ==========

    /**
     * 主键ID（雪花算法自动生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文章ID（外键 art_article.id）
     */
    private Long articleId;

    // ========== 统计数据 ==========

    /**
     * 浏览量
     */
    private Long viewCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 收藏数
     */
    private Integer collectCount;

    /**
     * 分享数
     */
    private Integer shareCount;

    // ========== 公共字段 ==========

    /**
     * 乐观锁版本
     */
    @Version
    private Integer version;

    /**
     * 创建人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新人ID
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标志
     */
    @TableLogic
    private Integer isDeleted;
}
