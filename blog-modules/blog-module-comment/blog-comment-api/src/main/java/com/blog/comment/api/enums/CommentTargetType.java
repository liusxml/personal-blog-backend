package com.blog.comment.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 评论目标类型枚举
 *
 * @author liusxml
 * @since 1.2.0
 */
@Getter
public enum CommentTargetType {

    ARTICLE("ARTICLE", "文章");

    @EnumValue
    private final String code;

    private final String description;

    CommentTargetType(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
