package com.blog.comment.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
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

    /**
     * 渲染后的 HTML 内容
     */
    private String contentHtml;

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

    /**
     * 是否已编辑
     */
    private Boolean isEdited;

    /**
     * 最后编辑时间
     */
    private LocalDateTime editTime;

    /**
     * 被@提及的用户ID列表（JSON格式，如"[123,456]"）
     */
    private String mentionedUserIds;

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
