package com.blog.comment.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 评论点赞事件
 *
 * @author liusxml
 * @since 1.5.0
 */
@Getter
public class CommentLikedEvent extends ApplicationEvent {

    private final Long commentId;

    public CommentLikedEvent(Long commentId) {
        super(commentId);
        this.commentId = commentId;
    }
}
