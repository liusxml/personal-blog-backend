package com.blog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章详情 VO（View Object）
 *
 * <p>
 * 用于返回文章完整信息，包括关联的分类、标签、作者和统计数据。
 * </p>
 *
 * <p>
 * 说明：
 * </p>
 * <ul>
 * <li>VO 仅用于响应，不实现 Identifiable 接口</li>
 * <li>使用 @Schema 注解生成 OpenAPI 文档</li>
 * <li>包含关联对象的嵌套 VO（避免多次查询）</li>
 * </ul>
 *
 * @author blog-system
 * @since 1.1.0
 */
@Data
@Schema(description = "文章详情VO")
public class ArticleDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文章ID")
    private String id; // Long序列化为String，避免精度丢失

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "文章内容（Markdown格式）")
    private String content;

    @Schema(description = "渲染后的HTML（如有）")
    private String contentHtml;

    @Schema(description = "封面图URL")
    private String coverImage;

    @Schema(description = "作者信息")
    private AuthorVO author;

    @Schema(description = "分类信息")
    private CategoryVO category;

    @Schema(description = "标签列表")
    private List<TagVO> tags;

    @Schema(description = "文章状态：0-草稿，2-已发布，3-已归档")
    private Integer status;

    @Schema(description = "文章类型：1-原创，2-转载，3-翻译")
    private Integer type;

    @Schema(description = "是否置顶")
    private Boolean isTop;

    @Schema(description = "是否精选")
    private Boolean isFeatured;

    @Schema(description = "是否禁止评论")
    private Boolean isCommentDisabled;

    @Schema(description = "原文链接（转载/翻译时）")
    private String originalUrl;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "目录结构（JSON）")
    private String tocJson;

    @Schema(description = "统计数据")
    private ArticleStatsVO stats;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 作者信息 VO
     */
    @Data
    @Schema(description = "作者信息")
    public static class AuthorVO implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "作者ID")
        private String id;

        @Schema(description = "用户名")
        private String username;

        @Schema(description = "昵称")
        private String nickname;

        @Schema(description = "头像URL")
        private String avatar;
    }

    /**
     * 分类信息 VO
     */
    @Data
    @Schema(description = "分类信息")
    public static class CategoryVO implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "分类ID")
        private String id;

        @Schema(description = "分类名称")
        private String name;

        @Schema(description = "URL标识")
        private String slug;

        @Schema(description = "分类图标")
        private String icon;
    }

    /**
     * 标签信息 VO
     */
    @Data
    @Schema(description = "标签信息")
    public static class TagVO implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "标签ID")
        private String id;

        @Schema(description = "标签名称")
        private String name;

        @Schema(description = "URL标识")
        private String slug;

        @Schema(description = "标签颜色")
        private String color;
    }

    /**
     * 统计数据 VO
     */
    @Data
    @Schema(description = "文章统计数据")
    public static class ArticleStatsVO implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "浏览量")
        private Long viewCount;

        @Schema(description = "点赞数")
        private Integer likeCount;

        @Schema(description = "评论数")
        private Integer commentCount;

        @Schema(description = "收藏数")
        private Integer collectCount;

        @Schema(description = "分享数")
        private Integer shareCount;
    }
}
