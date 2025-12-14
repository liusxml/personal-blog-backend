package com.blog.article.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章标签实体
 *
 * <p>
 * 映射数据库表 {@code art_tag}。
 * </p>
 *
 * @author blog-system
 * @since 1.1.0
 */
@Data
@TableName("art_tag")
public class ArticleTagEntity {

    // ========== 主键 ==========

    /**
     * 标签ID（雪花算法自动生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // ========== 基本信息 ==========

    /**
     * 标签名称
     */
    private String name;

    /**
     * URL标识
     */
    private String slug;

    /**
     * 标签颜色（HEX格式，如 #3B82F6）
     */
    private String color;

    /**
     * 标签描述
     */
    private String description;

    // ========== 统计 ==========

    /**
     * 关联文章数量（冗余字段，用于排序）
     */
    private Integer articleCount;

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
