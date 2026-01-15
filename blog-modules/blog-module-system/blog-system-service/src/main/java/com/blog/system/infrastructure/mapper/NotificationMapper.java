package com.blog.system.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.system.domain.entity.NotificationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 系统通知 Mapper
 *
 * @author liusxml
 * @since 1.6.0
 */
@Mapper
public interface NotificationMapper extends BaseMapper<NotificationEntity> {

    /**
     * 查询用户未读通知数量
     *
     * @param userId 用户ID
     * @return 未读通知数量
     */
    @Select("SELECT COUNT(*) FROM sys_notification " +
            "WHERE user_id = #{userId} AND is_read = 0 AND is_deleted = 0")
    Long countUnreadByUserId(Long userId);

    /**
     * 标记所有通知为已读
     *
     * @param userId 用户ID
     * @return 更新数量
     */
    @Update("UPDATE sys_notification " +
            "SET is_read = 1, read_time = NOW() " +
            "WHERE user_id = #{userId} AND is_read = 0 AND is_deleted = 0")
    int markAllAsRead(Long userId);
}
