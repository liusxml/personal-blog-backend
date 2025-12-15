package com.blog.comment.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.blog.comment.api.enums.CommentStatus;
import com.blog.comment.api.enums.CommentTargetType;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论实体
 *
 * @author liusxml
 * @since 1.2.0
 */
@Data
@TableName("cmt_comment")
public class CommentEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private CommentTargetType targetType;

    private Long targetId;

    private Long parentId;

    private String content;

    private CommentStatus status;

    private Integer likeCount;

    private Integer replyCount;

    private String path;

    private Integer depth;

    private Long rootId;

    /**
     * 审核/删除原因
     */
    private String auditReason;

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
