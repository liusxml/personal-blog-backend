package com.blog.comment.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.blog.comment.api.enums.ReportReasonType;
import com.blog.comment.api.enums.ReportStatus;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论举报实体
 *
 * @author liusxml
 * @since 1.5.0
 */
@Data
@TableName("cmt_comment_report")
public class CommentReportEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long commentId;

    private Long reporterId;

    private ReportReasonType reasonType;

    private String reasonDetail;

    private ReportStatus status;

    private String adminRemark;

    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
