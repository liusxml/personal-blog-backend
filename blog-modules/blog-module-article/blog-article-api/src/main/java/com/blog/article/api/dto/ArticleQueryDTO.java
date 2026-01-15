package com.blog.article.api.dto;

import com.blog.common.model.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文章查询参数
 *
 * <p>
 * 继承 {@link PageQuery} 获得分页参数，并扩展文章特定的筛选条件。
 * </p>
 *
 * <p>
 * 使用场景：
 * </p>
 * <ul>
 * <li>用户端文章列表查询</li>
 * <li>管理端文章管理列表</li>
 * <li>分类/标签筛选</li>
 * <li>关键词搜索</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文章查询参数")
public class ArticleQueryDTO extends PageQuery {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     * <p>
     * 筛选指定分类下的文章
     * </p>
     */
    @Schema(description = "分类ID", example = "1234567890123456789")
    private Long categoryId;

    /**
     * 标签ID
     * <p>
     * 筛选包含指定标签的文章
     * </p>
     */
    @Schema(description = "标签ID", example = "9876543210987654321")
    private Long tagId;

    /**
     * 关键词
     * <p>
     * 在标题和摘要中搜索（使用 MySQL FULLTEXT 或 LIKE）
     * </p>
     */
    @Schema(description = "关键词（搜索标题和摘要）", example = "Spring Boot")
    private String keyword;

    /**
     * 文章状态
     * <p>
     * 0-草稿, 1-已发布, 2-已归档
     * </p>
     * <p>
     * 用户端默认只查询已发布（status=1），管理端可指定
     * </p>
     */
    @Schema(description = "文章状态（0-草稿, 1-已发布, 2-已归档）", example = "1")
    private Integer status;

    /**
     * 作者ID
     * <p>
     * 筛选指定作者的文章（用于"我的文章"等场景）
     * </p>
     */
    @Schema(description = "作者ID", example = "1111111111111111111")
    private Long authorId;
}
