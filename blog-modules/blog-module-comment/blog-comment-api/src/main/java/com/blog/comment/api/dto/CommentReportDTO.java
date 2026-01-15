package com.blog.comment.api.dto;

import com.blog.comment.api.enums.ReportReasonType;
import com.blog.common.base.Identifiable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 评论举报 DTO
 *
 * @author liusxml
 * @since 1.5.0
 */
@Data
@Schema(description = "评论举报DTO")
public class CommentReportDTO implements Serializable, Identifiable<Long> {

    @Schema(description = "举报ID")
    private Long id;

    @Schema(description = "评论ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "评论ID不能为空")
    private Long commentId;

    @Schema(description = "举报原因类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "举报原因类型不能为空")
    private ReportReasonType reasonType;

    @Schema(description = "举报详细说明")
    @Size(max = 500, message = "举报详细说明不能超过500字")
    private String reasonDetail;
}
