package com.blog.system.constant;

/**
 * 角色常量
 *
 * @author liusxml
 * @since 1.0.0
 */
public final class RoleConstants {

    /**
     * 默认用户角色键
     */
    public static final String DEFAULT_USER_ROLE = "USER";
    /**
     * 管理员角色键
     */
    public static final String ADMIN_ROLE = "ADMIN";
    /**
     * 角色前缀（用于 Spring Security）
     */
    public static final String ROLE_PREFIX = "ROLE_";

    private RoleConstants() {
        // 工具类，禁止实例化
    }
}
