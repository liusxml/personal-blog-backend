package com.blog.common.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页查询参数基类
 *
 * <p>
 * 所有需要分页的查询 DTO 都应继承此类，以获得统一的分页参数。
 * </p>
 *
 * <p>
 * 使用示例：
 * </p>
 *
 * <pre>
 * {
 *     &#64;code
 *     &#64;Data
 *     @EqualsAndHashCode(callSuper = true)
 *     public class ArticleQueryDTO extends PageQuery {
 *         private Long categoryId;
 *         private String keyword;
 *     }
 * }
 * </pre>
 *
 * @author blog-system
 * @since 1.0.0
 */
@Data
@Schema(description = "分页查询参数")
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码（从1开始）
     */
    @Schema(description = "页码（从1开始）", example = "1")
    @Min(value = 1, message = "页码必须大于0")
    private Long current = 1L;

    /**
     * 每页数量
     */
    @Schema(description = "每页数量", example = "10")
    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    private Long size = 10L;

    /**
     * 排序字段（可选）
     * <p>
     * 格式：字段名，如 "createTime"
     * </p>
     */
    @Schema(description = "排序字段", example = "createTime")
    private String sortField;

    /**
     * 排序方向（可选）
     * <p>
     * 格式：asc 或 desc
     * </p>
     */
    @Schema(description = "排序方向（asc/desc）", example = "desc")
    private String sortOrder = "desc";
}
