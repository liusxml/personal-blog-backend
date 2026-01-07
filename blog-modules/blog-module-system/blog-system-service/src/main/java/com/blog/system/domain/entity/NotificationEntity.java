package com.blog.system.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.blog.common.enums.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统通知实体
 *
 * @author liusxml
 * @since 1.6.0
 */
@Data
@TableName("sys_notification")
public class NotificationEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 接收通知的用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 通知类型
     */
    @TableField("type")
    private NotificationType type;

    /**
     * 通知标题
     */
    @TableField("title")
    private String title;

    /**
     * 通知内容
     */
    @TableField("content")
    private String content;

    /**
     * 来源ID（评论ID、点赞ID等）
     */
    @TableField("source_id")
    private Long sourceId;

    /**
     * 来源类型：COMMENT, LIKE
     */
    @TableField("source_type")
    private String sourceType;

    /**
     * 是否已读：0-未读，1-已读
     */
    @TableField("is_read")
    private Boolean isRead;

    /**
     * 阅读时间
     */
    @TableField("read_time")
    private LocalDateTime readTime;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;

    /**
     * 触发人ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer isDeleted;
}
