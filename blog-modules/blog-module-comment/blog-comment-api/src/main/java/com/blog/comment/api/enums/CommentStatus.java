package com.blog.comment.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 评论状态枚举
 *
 * @author liusxml
 * @since 1.2.0
 */
@Getter
public enum CommentStatus {

    APPROVED(1, "已通过");

    @EnumValue // MyBatis-Plus: 数据库存储的值
    private final int code;

    private final String description;

    CommentStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
