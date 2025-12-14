package com.blog.article.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章实体
 *
 * <p>
 * 映射数据库表 {@code art_article}。
 * </p>
 *
 * <p>
 * 遵循项目规范：
 * </p>
 * <ul>
 * <li>使用 MyBatis-Plus 注解</li>
 * <li>包含所有 7 个公共字段</li>
 * <li>使用雪花算法生成 ID</li>
 * <li>支持乐观锁和逻辑删除</li>
 * <li>包含 MySQL 9.4 VECTOR 字段（embedding）</li>
 * </ul>
 *
 * <p>
 * 设计亮点：
 * </p>
 * <ul>
 * <li>embedding 字段：用于向量搜索（MySQL 9.4+）</li>
 * <li>tocJson：自动生成的目录结构</li>
 * <li>password：支持加密文章访问</li>
 * </ul>
 *
 * @author blog-system
 * @since 1.1.0
 */
@Data
@TableName("art_article")
public class ArticleEntity {

    // ========== 主键 ==========

    /**
     * 文章ID（雪花算法自动生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // ========== 基本信息 ==========

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章摘要（自动提取或手动填写）
     */
    private String summary;

    /**
     * 文章正文（Markdown格式）
     */
    private String content;

    /**
     * 渲染后HTML（可选，用于提升读取性能）
     */
    private String contentHtml;

    /**
     * 封面图URL或文件路径
     */
    private String coverImage;

    /**
     * 封面图文件ID（外键关联 file_file.id）
     */
    private Long coverImageId;

    // ========== 分类与作者 ==========

    /**
     * 作者ID（外键 sys_user.id）
     */
    private Long authorId;

    /**
     * 分类ID（外键 art_category.id）
     */
    private Long categoryId;

    // ========== 状态与类型 ==========

    /**
     * 状态: 0-草稿, 1-审核中, 2-已发布, 3-已归档
     */
    private Integer status;

    /**
     * 类型: 1-原创, 2-转载, 3-翻译
     */
    private Integer type;

    // ========== 特性标记 ==========

    /**
     * 是否置顶（0-否, 1-是）
     */
    private Integer isTop;

    /**
     * 是否精选（0-否, 1-是）
     */
    private Integer isFeatured;

    /**
     * 是否禁止评论（0-否, 1-是）
     */
    private Integer isCommentDisabled;

    // ========== 高级特性 ==========

    /**
     * 访问密码（加密存储，可选）
     */
    private String password;

    /**
     * 原文链接（转载/翻译时填写）
     */
    private String originalUrl;

    /**
     * 发布时间（首次发布时间）
     */
    private LocalDateTime publishTime;

    /**
     * 目录结构（JSON格式，自动生成）
     */
    private String tocJson;

    // ========== 向量搜索（⭐ MySQL 9.4 新特性）==========

    /**
     * 文章内容向量（用于语义搜索和推荐）
     * <p>
     * 使用 MySQL 9.4 VECTOR 类型，维度为 1536（OpenAI text-embedding-3-small）
     * </p>
     * <p>
     * 注意：MyBatis-Plus 会将其映射为 String 类型
     * </p>
     */
    @TableField(typeHandler = org.apache.ibatis.type.StringTypeHandler.class)
    private String embedding;

    // ========== 公共字段（必须包含）==========

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
     * 逻辑删除：0=未删除、1=已删除
     */
    @TableLogic
    private Integer isDeleted;
}
