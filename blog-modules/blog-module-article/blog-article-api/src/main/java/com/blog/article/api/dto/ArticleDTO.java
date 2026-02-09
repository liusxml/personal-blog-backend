package com.blog.article.api.dto;

import com.blog.common.base.Identifiable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 文章创建/更新 DTO
 *
 * <p>
 * 用于创建新文章和更新现有文章的请求对象。
 * </p>
 *
 * <p>
 * 遵循项目规范：
 * </p>
 * <ul>
 * <li>实现 Serializable 接口</li>
 * <li>实现 Identifiable&lt;Long&gt; 接口（BaseServiceImpl 要求）</li>
 * <li>使用 @Schema 注解（OpenAPI 文档）</li>
 * <li>使用 Jakarta Validation 注解进行参数校验</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Data
@Schema(description = "文章创建/更新DTO")
public class ArticleDTO implements Serializable, Identifiable<Long> {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文章ID（更新时必填，创建时留空）")
    private Long id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255字符")
    @Schema(description = "文章标题", example = "Spring Boot 最佳实践")
    private String title;

    @Schema(description = "文章摘要（留空则自动从内容提取）", example = "本文介绍Spring Boot开发中的最佳实践...")
    @Size(max = 500, message = "摘要长度不能超过500字符")
    private String summary;

    @NotBlank(message = "内容不能为空")
    @Schema(description = "文章正文（Markdown格式）", example = "# 引言\\n\\n本文将介绍...")
    private String content;

    @Schema(description = "封面图URL")
    @Size(max = 500, message = "封面图URL长度不能超过500字符")
    private String coverImage;

    @Schema(description = "封面图文件ID（关联 file_file 表）")
    private Long coverImageId;

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "标签ID列表")
    private List<Long> tagIds;

    @Schema(description = "文章类型：1-原创，2-转载，3-翻译", example = "1")
    private Integer type;

    @Schema(description = "原文链接（转载/翻译时填写）")
    @Size(max = 500, message = "原文链接长度不能超过500字符")
    private String originalUrl;

    @Schema(description = "是否置顶（0-否，1-是）", example = "0")
    private Integer isTop;

    @Schema(description = "是否精选（0-否，1-是）", example = "0")
    private Integer isFeatured;

    @Schema(description = "是否禁止评论（0-否，1-是）", example = "0")
    private Integer isCommentDisabled;

    @Schema(description = "访问密码（可选，用于加密文章）")
    @Size(max = 100, message = "密码长度不能超过100字符")
    private String password;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
