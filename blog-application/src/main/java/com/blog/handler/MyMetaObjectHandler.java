package com.blog.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.blog.common.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 元数据自动填充处理器
 * <p>
 * 自动填充字段：
 * <ul>
 * <li>createTime - 创建时间（插入时）</li>
 * <li>updateTime - 更新时间（插入和更新时）</li>
 * <li>createBy - 创建者ID（插入时，从Security Context获取）</li>
 * <li>updateBy - 更新者ID（插入和更新时，从Security Context获取）</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时自动填充
     *
     * @param metaObject 元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("插入数据自动填充: {}", metaObject.getOriginalObject().getClass().getSimpleName());

        // 填充时间字段
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);

        // 填充操作人ID（从Security Context获取）
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            this.strictInsertFill(metaObject, "createBy", () -> currentUserId, Long.class);
            this.strictInsertFill(metaObject, "updateBy", () -> currentUserId, Long.class);
        }
    }

    /**
     * 更新时自动填充
     *
     * @param metaObject 元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("更新数据自动填充: {}", metaObject.getOriginalObject().getClass().getSimpleName());

        // 填充更新时间
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);

        // 填充更新人ID（从Security Context获取）
        Long currentUserId = getCurrentUserId();
        if (currentUserId != null) {
            this.strictUpdateFill(metaObject, "updateBy", () -> currentUserId, Long.class);
        }
    }

    /**
     * 获取当前登录用户ID
     * <p>
     * 从Spring Security上下文中获取当前用户ID。
     * 如果用户未登录或在非Web环境（如定时任务）中，返回null。
     *
     * @return 当前用户ID，未登录时返回null
     */
    private Long getCurrentUserId() {
        try {
            return SecurityUtils.getCurrentUserId();
        } catch (Exception e) {
            // 在非Web环境（如定时任务、系统初始化）中可能获取不到用户ID
            log.debug("无法从Security Context获取用户ID: {}", e.getMessage());
            return null;
        }
    }
}
