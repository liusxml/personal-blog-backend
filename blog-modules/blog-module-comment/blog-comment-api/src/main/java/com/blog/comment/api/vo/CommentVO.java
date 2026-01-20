package com.blog.comment.api.vo;

import com.blog.comment.api.enums.CommentStatus;
import com.blog.comment.api.enums.CommentTargetType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论响应VO
 *
 * @author liusxml
 * @since 1.2.0
 */
@Data
@Schema(description = "评论响应对象")
public class CommentVO implements Serializable {

    @Schema(description = "评论ID")
    private String id; // Long序列化为String，避免精度丢失

    @Schema(description = "目标类型")
    private CommentTargetType targetType;

    @Schema(description = "目标ID")
    private String targetId; // Long序列化为String，避免精度丢失

    @Schema(description = "父评论ID")
    private String parentId; // Long序列化为String，避免精度丢失

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "状态")
    private CommentStatus status;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "回复数")
    private Integer replyCount;

    @Schema(description = "评论者ID")
    private String createBy; // Long序列化为String，避免精度丢失

    @Schema(description = "评论时间", example = "2025-12-15 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2025-12-15 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
