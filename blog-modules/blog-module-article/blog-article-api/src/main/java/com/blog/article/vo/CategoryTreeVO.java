package com.blog.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类树VO
 * 用于返回树形结构的分类
 *
 * @author liusxml
 * @since 1.7.0
 */
@Data
@Schema(description = "分类树VO")
public class CategoryTreeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "分类ID")
    private String id; // Long序列化为String，避免精度丢失

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "URL标识")
    private String slug;

    @Schema(description = "分类图标")
    private String icon;

    @Schema(description = "分类描述")
    private String description;

    @Schema(description = "父分类ID")
    private String parentId; // Long序列化为String，避免精度丢失

    @Schema(description = "分类路径")
    private String path;

    @Schema(description = "排序权重")
    private Integer sortOrder;

    @Schema(description = "子分类列表")
    private List<CategoryTreeVO> children;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
