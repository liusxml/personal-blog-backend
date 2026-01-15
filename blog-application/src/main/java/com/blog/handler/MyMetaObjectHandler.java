package com.blog.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.blog.common.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus元数据自动填充处理器
 * <p>
 * 实现审计字段的自动填充，无需在业务代码中手动设置。
 *
 * <h3>自动填充字段</h3>
 * <ul>
 * <li><b>createTime</b> - 创建时间（INSERT时填充）</li>
 * <li><b>updateTime</b> - 更新时间（INSERT和UPDATE时填充）</li>
 * <li><b>createBy</b> - 创建者ID（INSERT时填充，从Security上下文获取）</li>
 * <li><b>updateBy</b> - 更新者ID（INSERT和UPDATE时填充，从Security上下文获取）</li>
 * </ul>
 *
 * <h3>使用方法</h3>
 * 在Entity字段上添加注解：
 *
 * <pre>
 * &#64;TableField(fill = FieldFill.INSERT)
 * private LocalDateTime createTime;
 *
 * &#64;TableField(fill = FieldFill.INSERT_UPDATE)
 * private LocalDateTime updateTime;
 * </pre>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {

    // ========================================================================
    // 字段名常量 - 避免硬编码，便于维护
    // ========================================================================

    /**
     * 创建时间字段名
     */
    private static final String FIELD_CREATE_TIME = "createTime";

    /**
     * 更新时间字段名
     */
    private static final String FIELD_UPDATE_TIME = "updateTime";

    /**
     * 创建者ID字段名
     */
    private static final String FIELD_CREATE_BY = "createBy";

    /**
     * 更新者ID字段名
     */
    private static final String FIELD_UPDATE_BY = "updateBy";

    // ========================================================================
    // MetaObjectHandler接口实现
    // ========================================================================

    /**
     * 插入操作时的字段自动填充
     * <p>
     * 填充策略：
     * <ol>
     * <li>createTime、updateTime - 填充当前时间</li>
     * <li>createBy、updateBy - 填充当前登录用户ID（若可获取）</li>
     * </ol>
     *
     * @param metaObject 元对象，包含实体信息
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        if (log.isDebugEnabled()) {
            log.debug("自动填充[INSERT] - 实体: {}",
                    metaObject.getOriginalObject().getClass().getSimpleName());
        }

        // 填充时间字段（必填）
        this.strictInsertFill(metaObject, FIELD_CREATE_TIME, LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, FIELD_UPDATE_TIME, LocalDateTime::now, LocalDateTime.class);

        // 填充操作人ID（可选，需登录态）
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            this.strictInsertFill(metaObject, FIELD_CREATE_BY, () -> currentUserId, Long.class);
            this.strictInsertFill(metaObject, FIELD_UPDATE_BY, () -> currentUserId, Long.class);
        }
    }

    /**
     * 更新操作时的字段自动填充
     * <p>
     * 填充策略：
     * <ol>
     * <li>updateTime - 填充当前时间</li>
     * <li>updateBy - 填充当前登录用户ID（若可获取）</li>
     * </ol>
     *
     * @param metaObject 元对象，包含实体信息
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        if (log.isDebugEnabled()) {
            log.debug("自动填充[UPDATE] - 实体: {}",
                    metaObject.getOriginalObject().getClass().getSimpleName());
        }

        // 填充更新时间（必填）
        this.strictUpdateFill(metaObject, FIELD_UPDATE_TIME, LocalDateTime::now, LocalDateTime.class);

        // 填充更新人ID（可选，需登录态）
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            this.strictUpdateFill(metaObject, FIELD_UPDATE_BY, () -> currentUserId, Long.class);
        }
    }

    // ========================================================================
    // 私有辅助方法
    // ========================================================================

    /**
     * 获取当前登录用户ID
     * <p>
     * 从Spring Security上下文中获取当前用户ID。
     * <ul>
     * <li>成功场景：Web请求中，用户已登录</li>
     * <li>失败场景：定时任务、系统初始化、未登录用户</li>
     * </ul>
     *
     * @return 当前用户ID，获取失败时返回null
     */
    private Long getCurrentUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            // 在非Web环境或未登录场景下，无法获取用户ID是正常行为
            log.debug("无法获取当前用户ID: {} - 原因: {}",
                    e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }
}
