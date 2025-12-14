package com.blog.enums;

import lombok.Getter;

/**
 * 文章状态枚举
 *
 * <p>
 * 定义文章的生命周期状态，支持状态模式实现。
 * </p>
 *
 * <p>
 * 状态流转规则：
 * </p>
 * <ul>
 * <li>草稿(DRAFT) → 已发布(PUBLISHED)</li>
 * <li>已发布(PUBLISHED) → 已归档(ARCHIVED)</li>
 * <li>已归档(ARCHIVED) → 已发布(PUBLISHED)</li>
 * <li>任意状态 → 逻辑删除(通过 is_deleted 标志)</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.1.0
 */
@Getter
public enum ArticleStatus {

    /**
     * 草稿状态
     * <p>
     * 文章创建后的默认状态，仅作者可见
     * </p>
     */
    DRAFT(0, "草稿"),

    /**
     * 审核中状态（预留）
     * <p>
     * 未来如需增加审核流程，可启用此状态
     * </p>
     */
    UNDER_REVIEW(1, "审核中"),

    /**
     * 已发布状态
     * <p>
     * 文章公开可见，用户可以阅读和评论
     * </p>
     */
    PUBLISHED(2, "已发布"),

    /**
     * 已归档状态
     * <p>
     * 文章不再显示在列表中，但可通过直接链接访问
     * </p>
     */
    ARCHIVED(3, "已归档");

    /**
     * 状态码（存储在数据库中）
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String description;

    ArticleStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return 对应的枚举实例
     * @throws IllegalArgumentException 如果状态码无效
     */
    public static ArticleStatus of(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("状态码不能为空");
        }
        for (ArticleStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的文章状态码: " + code);
    }

    /**
     * 判断是否允许公开访问
     *
     * @return true 如果状态为已发布或已归档
     */
    public boolean isPublic() {
        return this == PUBLISHED || this == ARCHIVED;
    }

    /**
     * 判断是否可以编辑
     *
     * @return true 如果状态为草稿或审核中
     */
    public boolean isEditable() {
        return this == DRAFT || this == UNDER_REVIEW;
    }
}
