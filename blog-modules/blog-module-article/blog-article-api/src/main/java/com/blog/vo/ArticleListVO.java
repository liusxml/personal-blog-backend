package com.blog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章列表 VO（精简版）
 *
 * <p>
 * 用于文章列表查询，不包含完整内容，提升性能。
 * </p>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Data
@Schema(description = "文章列表VO")
public class ArticleListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文章ID")
    private String id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "封面图URL")
    private String coverImage;

    @Schema(description = "作者昵称")
    private String authorName;

    @Schema(description = "作者ID")
    private String authorId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "文章类型：1-原创，2-转载，3-翻译")
    private Integer type;

    @Schema(description = "是否置顶")
    private Boolean isTop;

    @Schema(description = "是否精选")
    private Boolean isFeatured;

    @Schema(description = "浏览量")
    private Long viewCount;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
