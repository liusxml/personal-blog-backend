package com.blog.comment.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论编辑历史实体
 *
 * @author liusxml
 * @since 1.6.0
 */
@Data
@TableName("cmt_comment_history")
public class CommentHistoryEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 评论ID
     */
    @TableField("comment_id")
    private Long commentId;

    /**
     * 编辑前的原始Markdown内容
     */
    @TableField("old_content")
    private String oldContent;

    /**
     * 编辑前的HTML内容
     */
    @TableField("old_content_html")
    private String oldContentHtml;

    /**
     * 编辑原因（用户可选填写）
     */
    @TableField("edit_reason")
    private String editReason;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;

    /**
     * 编辑人ID（冗余存储）
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 编辑时间
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
