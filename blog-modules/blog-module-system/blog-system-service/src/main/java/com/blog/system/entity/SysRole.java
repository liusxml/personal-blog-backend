package com.blog.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体
 *
 * @author liusxml
 * @since 1.0.0
 */
@Data
@TableName("sys_role")
public class SysRole {

    /**
     * 角色ID (主键, 雪花算法)
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色权限字符串
     */
    private String roleKey;

    /**
     * 角色状态 (1-正常, 0-停用)
     */
    private Integer status;

    /**
     * 版本号 (乐观锁)
     */
    @Version
    private Integer version;

    /**
     * 创建者ID
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新者ID
     */
    private Long updateBy;

    /**
     * 更新时间
     */
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
