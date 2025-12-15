package com.blog.comment.api.dto;

import com.blog.comment.api.enums.CommentTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 评论查询参数
 *
 * @author liusxml
 * @since 1.2.0
 */
@Data
@Schema(description = "评论查询参数")
public class CommentQueryDTO implements Serializable {

    @Schema(description = "目标类型")
    private CommentTargetType targetType;

    @Schema(description = "目标ID")
    private Long targetId;

    @Schema(description = "评论者ID")
    private Long createBy;

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "20")
    private Integer pageSize = 20;
}
