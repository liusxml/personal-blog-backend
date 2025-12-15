package com.blog.comment.domain.event;

import com.blog.comment.infrastructure.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 评论事件监听器
 *
 * @author liusxml
 * @since 1.5.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentEventListener {

    private final CommentMapper commentMapper;

    /**
     * 处理点赞事件（异步）
     */
    @Async
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleCommentLiked(CommentLikedEvent event) {
        log.info("处理点赞事件: commentId={}", event.getCommentId());

        // 更新点赞数 +1
        commentMapper.incrementLikeCount(event.getCommentId());
    }

    /**
     * 处理取消点赞事件（异步）
     */
    @Async
    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void handleCommentUnliked(CommentUnlikedEvent event) {
        log.info("处理取消点赞事件: commentId={}", event.getCommentId());

        // 更新点赞数 -1
        commentMapper.decrementLikeCount(event.getCommentId());
    }

    /**
     * 处理举报事件（异步）
     */
    @Async
    @EventListener
    public void handleCommentReported(CommentReportedEvent event) {
        log.warn("评论被举报: commentId={}, reportId={}",
                event.getCommentId(), event.getReportId());

        // TODO: 发送通知给管理员
    }
}
