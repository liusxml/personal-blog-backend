package com.blog.comment.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 评论举报事件
 *
 * @author liusxml
 * @since 1.5.0
 */
@Getter
public class CommentReportedEvent extends ApplicationEvent {

    private final Long commentId;
    private final Long reportId;

    public CommentReportedEvent(Long commentId, Long reportId) {
        super(commentId);
        this.commentId = commentId;
        this.reportId = reportId;
    }
}
