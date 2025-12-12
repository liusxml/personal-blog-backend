package com.blog.common.constants;

/**
 * Redis 缓存键常量
 * <p>
 * 定义项目中所有 Redis 缓存键的前缀，确保键命名的一致性和可维护性。
 * <p>
 * <b>命名规范：</b>
 * <ul>
 * <li>使用冒号（:）分隔命名空间</li>
 * <li>格式：{业务模块}:{数据类型}:</li>
 * <li>示例：user:roles:, role:detail:</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
public final class CacheKeys {

    private CacheKeys() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ============================= User Module (用户模块)
    // =============================

    /**
     * 用户角色列表缓存键前缀
     * <p>
     * 完整键格式：user:roles:{userId}
     * <p>
     * 存储内容：用户拥有的所有角色列表
     */
    public static final String USER_ROLES_PREFIX = "user:roles:";

    // ============================= Role Module (角色模块)
    // =============================

    /**
     * 角色详情缓存键前缀
     * <p>
     * 完整键格式：role:detail:{roleId}
     * <p>
     * 存储内容：角色的详细信息（SysRole 实体）
     */
    public static final String ROLE_DETAIL_PREFIX = "role:detail:";

    // ============================= Helper Methods (辅助方法)
    // =============================

    /**
     * 构建用户角色缓存键
     *
     * @param userId 用户ID
     * @return 完整的缓存键，例如：user:roles:1
     */
    public static String userRolesKey(Long userId) {
        return USER_ROLES_PREFIX + userId;
    }

    /**
     * 构建角色详情缓存键
     *
     * @param roleId 角色ID
     * @return 完整的缓存键，例如：role:detail:1
     */
    public static String roleDetailKey(Long roleId) {
        return ROLE_DETAIL_PREFIX + roleId;
    }
}
