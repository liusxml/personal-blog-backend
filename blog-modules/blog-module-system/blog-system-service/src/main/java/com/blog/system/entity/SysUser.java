package com.blog.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体
 * <p>
 * 建议数据库索引：
 * <ul>
 * <li><b>idx_username</b> (username) - 登录查询、唯一性约束</li>
 * <li><b>idx_email</b> (email) - 邮箱登录、唯一性约束</li>
 * <li><b>idx_status</b> (status) - 用户状态筛选</li>
 * <li><b>idx_create_time</b> (create_time) - 按注册时间排序</li>
 * </ul>
 * <p>
 * 性能优化建议：
 * <ul>
 * <li>username 和 email 应设置为 UNIQUE 索引</li>
 * <li>登录高峰期可考虑为 (username, status) 创建联合索引</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Data
@TableName("sys_user")
public class SysUser {

    /**
     * 用户ID (主键, 雪花算法)
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户名 (登录凭证)
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 加密后的密码
     */
    private String password;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 用户头像URL
     */
    private String avatar;

    /**
     * 账户状态 (1-正常, 0-禁用)
     */
    private Integer status;

    /**
     * 版本号 (乐观锁)
     */
    @Version
    private Integer version;

    /**
     * 创建者ID（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新者ID（自动填充）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 更新时间（自动填充）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除 (0-未删, 1-已删)
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * 备注
     */
    private String remark;
}
