package com.blog.comment.api.dto;

import com.blog.comment.api.enums.CommentTargetType;
import com.blog.common.base.Identifiable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 评论请求DTO
 *
 * @author liusxml
 * @since 1.2.0
 */
@Data
@Schema(description = "评论请求对象")
public class CommentDTO implements Serializable, Identifiable<Long> {

    @Schema(description = "评论ID (更新时必填)")
    private Long id;

    @NotNull(message = "目标类型不能为空")
    @Schema(description = "评论目标类型", example = "ARTICLE", requiredMode = Schema.RequiredMode.REQUIRED)
    private CommentTargetType targetType;

    @NotNull(message = "目标ID不能为空")
    @Schema(description = "目标ID (文章ID)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long targetId;

    @Schema(description = "父评论ID (回复评论时填写)", example = "123")
    private Long parentId;

    @NotBlank(message = "评论内容不能为空")
    @Schema(description = "评论内容", example = "这篇文章写得很好！", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
