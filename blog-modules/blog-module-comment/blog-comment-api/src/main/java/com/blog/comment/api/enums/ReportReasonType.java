package com.blog.comment.api.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 举报原因类型枚举
 *
 * @author liusxml
 * @since 1.5.0
 */
@Getter
@RequiredArgsConstructor
public enum ReportReasonType {

    /**
     * 垃圾广告
     */
    SPAM("垃圾广告"),

    /**
     * 辱骂诽谤
     */
    ABUSE("辱骂诽谤"),

    /**
     * 违法信息
     */
    ILLEGAL("违法信息"),

    /**
     * 色情低俗
     */
    PORNOGRAPHY("色情低俗"),

    /**
     * 虚假信息
     */
    FALSE_INFO("虚假信息"),

    /**
     * 其他
     */
    OTHER("其他");

    /**
     * 描述文本（用于前端展示）
     * 数据库存储 name()（如 SPAM），JSON 返回 description（如 "垃圾广告"）
     */
    @JsonValue
    private final String description;
}
