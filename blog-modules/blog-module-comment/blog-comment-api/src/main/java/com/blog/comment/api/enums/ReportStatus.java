package com.blog.comment.api.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 举报审核状态枚举
 *
 * @author liusxml
 * @since 1.5.0
 */
@Getter
@RequiredArgsConstructor
public enum ReportStatus {

    /**
     * 待审核
     */
    PENDING("待审核"),

    /**
     * 有效举报
     */
    APPROVED("有效"),

    /**
     * 无效举报
     */
    REJECTED("无效");

    /**
     * 描述文本（用于前端展示）
     * 数据库存储 name()（如 PENDING），JSON 返回 description（如 "待审核"）
     */
    @JsonValue
    private final String description;
}
