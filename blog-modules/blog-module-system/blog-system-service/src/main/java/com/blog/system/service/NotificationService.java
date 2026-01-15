package com.blog.system.service;

import com.blog.common.enums.NotificationType;
import com.blog.system.api.service.INotificationService;
import com.blog.system.domain.entity.NotificationEntity;
import com.blog.system.infrastructure.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 通知服务
 *
 * @author liusxml
 * @since 1.6.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements INotificationService {

    private final NotificationMapper notificationMapper;

    /**
     * 创建@提及通知
     *
     * @param commentId   评论ID
     * @param userIds     被提及的用户ID集合
     * @param mentionerId 提及人ID
     */
    @Override
    public void createMentionNotifications(Long commentId, Set<Long> userIds, Long mentionerId) {
        for (Object userIdObj : userIds) {
            // 安全地将元素转换为 Long（处理 Jackson 反序列化可能导致的类型问题）
            Long userId = convertToLong(userIdObj);

            if (userId.equals(mentionerId)) {
                continue; // 不给自己发通知
            }

            NotificationEntity notification = new NotificationEntity();
            notification.setUserId(userId);
            notification.setType(NotificationType.USER_MENTION);
            notification.setTitle("您被@提及了");
            notification.setContent("有人在评论中@了你");
            notification.setSourceId(commentId);
            notification.setSourceType("COMMENT");
            notification.setIsRead(false);

            notificationMapper.insert(notification);
        }
    }

    /**
     * 安全地将对象转换为 Long
     * 处理 Jackson 反序列化可能导致的类型问题（Integer、String 等）
     */
    private Long convertToLong(Object obj) {
        if (obj instanceof Long) {
            return (Long) obj;
        } else if (obj instanceof Number) {
            return ((Number) obj).longValue();
        } else if (obj instanceof String) {
            return Long.parseLong((String) obj);
        } else {
            throw new IllegalArgumentException("Cannot convert to Long: " + obj);
        }
    }

    /**
     * 创建回复通知
     *
     * @param commentId    评论ID
     * @param parentUserId 父评论作者ID
     * @param replierId    回复人ID
     */
    @Override
    public void createReplyNotification(Long commentId, Long parentUserId, Long replierId) {
        if (parentUserId.equals(replierId)) {
            return; // 回复自己不通知
        }

        NotificationEntity notification = new NotificationEntity();
        notification.setUserId(parentUserId);
        notification.setType(NotificationType.COMMENT_REPLY);
        notification.setTitle("您的评论收到了新回复");
        notification.setSourceId(commentId);
        notification.setSourceType("COMMENT");
        notification.setIsRead(false);

        notificationMapper.insert(notification);
        log.info("创建回复通知: commentId={}, userId={}", commentId, parentUserId);
    }
}
