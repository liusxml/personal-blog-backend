package com.blog.comment.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

/**
 * 用户被@提及事件
 *
 * @author liusxml
 * @since 1.6.0
 */
@Getter
@AllArgsConstructor
public class UserMentionedEvent {
    /**
     * 评论ID
     */
    private final Long commentId;

    /**
     * 被@提及的用户ID集合
     */
    private final Set<Long> mentionedUserIds;

    /**
     * 提及人ID
     */
    private final Long mentionerId;
}
