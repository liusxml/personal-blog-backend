package com.blog.comment.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评论回复事件
 *
 * @author liusxml
 * @since 1.6.0
 */
@Getter
@AllArgsConstructor
public class CommentRepliedEvent {
    /**
     * 新评论ID
     */
    private final Long commentId;

    /**
     * 父评论ID
     */
    private final Long parentCommentId;

    /**
     * 被回复者ID（父评论的作者）
     */
    private final Long repliedUserId;

    /**
     * 回复者ID（新评论的作者）
     */
    private final Long replierId;
}
