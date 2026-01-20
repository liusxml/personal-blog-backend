package com.blog.article.dto;

import com.blog.common.base.Identifiable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分类DTO
 * 用于创建和更新分类
 *
 * @author liusxml
 * @since 1.7.0
 */
@Data
@Schema(description = "分类DTO")
public class CategoryDTO implements Serializable, Identifiable<Long> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "分类ID（更新时必填）")
    private Long id;

    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "Java")
    private String name;

    @Schema(description = "URL标识（可选，自动生成）", example = "java")
    private String slug;

    @Schema(description = "分类图标", example = "☕")
    private String icon;

    @Schema(description = "分类描述", example = "Java编程技术")
    private String description;

    @Schema(description = "父分类ID（顶级分类为null）")
    private Long parentId;

    @Schema(description = "排序权重（数字越小越靠前）", example = "1")
    private Integer sortOrder;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
