package com.blog.comment.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 评论审核 DTO
 *
 * @author liusxml
 * @since 1.3.0
 */
@Data
@Schema(description = "评论审核对象")
public class CommentAuditDTO implements Serializable {

    @Schema(description = "拒绝/删除原因", example = "内容违规")
    @NotBlank(message = "原因不能为空")
    private String reason;
}
