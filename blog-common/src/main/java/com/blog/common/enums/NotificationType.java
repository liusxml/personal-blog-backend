package com.blog.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通知类型枚举
 *
 * @author liusxml
 * @since 1.6.0
 */
@Getter
@RequiredArgsConstructor
public enum NotificationType {

    /**
     * 评论回复通知
     */
    COMMENT_REPLY("回复通知"),

    /**
     * @提及通知
     */
    USER_MENTION("@提及通知"),

    /**
     * 评论点赞通知（可选）
     */
    COMMENT_LIKE("点赞通知");

    /**
     * 描述文本（用于前端展示）
     * 数据库存储 name()（如 COMMENT_REPLY），JSON 返回 description（如 "回复通知"）
     */
    @JsonValue
    private final String description;
}
