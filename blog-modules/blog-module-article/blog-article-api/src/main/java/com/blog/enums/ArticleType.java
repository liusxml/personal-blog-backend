package com.blog.enums;

import lombok.Getter;

/**
 * 文章类型枚举
 *
 * <p>
 * 区分文章的来源和版权属性。
 * </p>
 *
 * @author blog-system
 * @since 1.1.0
 */
@Getter
public enum ArticleType {

    /**
     * 原创文章
     */
    ORIGINAL(1, "原创"),

    /**
     * 转载文章
     * <p>
     * 需要填写 original_url 字段
     * </p>
     */
    REPRINT(2, "转载"),

    /**
     * 翻译文章
     * <p>
     * 需要填写 original_url 字段
     * </p>
     */
    TRANSLATION(3, "翻译");

    private final Integer code;
    private final String description;

    ArticleType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据类型码获取枚举
     *
     * @param code 类型码
     * @return 对应的枚举实例
     */
    public static ArticleType of(Integer code) {
        if (code == null) {
            return ORIGINAL; // 默认原创
        }
        for (ArticleType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("无效的文章类型码: " + code);
    }

    /**
     * 判断是否需要标注原文链接
     *
     * @return true 如果是转载或翻译
     */
    public boolean requiresOriginalUrl() {
        return this == REPRINT || this == TRANSLATION;
    }
}
