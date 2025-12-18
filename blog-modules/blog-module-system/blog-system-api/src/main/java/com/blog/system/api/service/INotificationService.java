package com.blog.system.api.service;

import java.util.Set;

/**
 * 通知服务接口
 * <p>
 * 定义通知创建的契约，供其他模块（如评论模块）通过接口调用。
 * 遵循模块化架构原则：Service -> API -> Common
 *
 * @author liusxml
 * @since 1.6.0
 */
public interface INotificationService {

    /**
     * 创建@提及通知
     *
     * @param commentId   评论ID
     * @param userIds     被提及的用户ID集合
     * @param mentionerId 提及人ID
     */
    void createMentionNotifications(Long commentId, Set<Long> userIds, Long mentionerId);

    /**
     * 创建回复通知
     *
     * @param commentId    评论ID
     * @param parentUserId 父评论作者ID
     * @param replierId    回复人ID
     */
    void createReplyNotification(Long commentId, Long parentUserId, Long replierId);
}
