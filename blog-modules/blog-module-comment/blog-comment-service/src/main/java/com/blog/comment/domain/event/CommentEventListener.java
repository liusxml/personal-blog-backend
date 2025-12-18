package com.blog.comment.domain.event;

import com.blog.comment.infrastructure.mapper.CommentMapper;
import com.blog.system.api.service.INotificationService;
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
    private final INotificationService notificationService;

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

        // TODO: Phase 7 - 发送通知给管理员
    }

    /**
     * 处理@提及事件（异步）
     * <p>
     * Phase 7: 集成通知服务，为被提及的用户创建通知
     */
    @Async
    @EventListener
    public void handleUserMentioned(UserMentionedEvent event) {
        log.info("处理@提及事件: commentId={}, 提及用户数={}, mentionerId={}",
                event.getCommentId(),
                event.getMentionedUserIds().size(),
                event.getMentionerId());

        // ✅ Phase 7: 创建@提及通知
        notificationService.createMentionNotifications(
                event.getCommentId(),
                event.getMentionedUserIds(),
                event.getMentionerId());
    }

    /**
     * 处理评论回复事件（异步）
     * <p>
     * Phase 7: 集成通知服务，为被回复用户创建通知
     */
    @Async
    @EventListener
    public void handleCommentReplied(CommentRepliedEvent event) {
        log.info("处理回复事件: commentId={}, parentId={}, repliedUserId={}, replierId={}",
                event.getCommentId(),
                event.getParentCommentId(),
                event.getRepliedUserId(),
                event.getReplierId());

        // ✅ Phase 7: 创建回复通知
        notificationService.createReplyNotification(
                event.getCommentId(),
                event.getRepliedUserId(),
                event.getReplierId());
    }
}
