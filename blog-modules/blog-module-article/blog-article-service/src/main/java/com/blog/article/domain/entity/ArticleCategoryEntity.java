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
 * 文章分类实体
 *
 * <p>
 * 映射数据库表 {@code art_category}。
 * </p>
 *
 * <p>
 * 支持多级分类：
 * </p>
 * <ul>
 * <li>使用 parentId 建立父子关系</li>
 * <li>使用 path 字段便于查询所有子分类（格式：/1/2/3）</li>
 * <li>支持自定义排序权重</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.1.0
 */
@Data
@TableName("art_category")
public class ArticleCategoryEntity {

    // ========== 主键 ==========

    /**
     * 分类ID（雪花算法自动生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // ========== 基本信息 ==========

    /**
     * 分类名称
     */
    private String name;

    /**
     * URL友好标识（用于生成链接）
     */
    private String slug;

    /**
     * 分类图标（图标名称或URL）
     */
    private String icon;

    /**
     * 分类描述
     */
    private String description;

    // ========== 层级关系 ==========

    /**
     * 父分类ID（NULL表示顶级分类）
     */
    private Long parentId;

    /**
     * 分类路径（便于查询子分类，格式: /1/2/3）
     */
    private String path;

    // ========== 排序 ==========

    /**
     * 排序权重（数字越小越靠前）
     */
    private Integer sortOrder;

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
