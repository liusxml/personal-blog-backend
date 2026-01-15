package com.blog.comment.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 评论状态枚举（扩展版）
 *
 * @author liusxml
 * @since 1.3.0
 */
@Getter
@Schema(description = "评论状态")
public enum CommentStatus {

    /**
     * 待审核 - 新评论的初始状态
     */
    PENDING(0, "待审核", true),

    /**
     * 已通过 - 审核通过，前端可见
     */
    APPROVED(1, "已通过", true),

    /**
     * 已拒绝 - 审核不通过，前端不可见
     */
    REJECTED(2, "已拒绝", false),

    /**
     * 用户删除 - 用户主动删除（软删除）
     */
    USER_DELETED(3, "用户删除", false),

    /**
     * 管理员删除 - 管理员删除（违规内容）
     */
    ADMIN_DELETED(4, "管理员删除", false);

    @EnumValue // MyBatis-Plus: 数据库存储的值
    private final int code;

    private final String description;

    /**
     * 前端是否可见
     */
    private final boolean visible;

    CommentStatus(int code, String description, boolean visible) {
        this.code = code;
        this.description = description;
        this.visible = visible;
    }

    /**
     * 判断是否为已删除状态
     */
    public boolean isDeleted() {
        return this == USER_DELETED || this == ADMIN_DELETED;
    }

    /**
     * 判断是否可审核
     */
    public boolean canAudit() {
        return this == PENDING;
    }
}
