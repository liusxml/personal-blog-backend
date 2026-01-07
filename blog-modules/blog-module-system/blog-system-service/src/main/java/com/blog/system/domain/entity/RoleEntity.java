package com.blog.system.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体
 * <p>
 * 建议数据库索引：
 * <ul>
 * <li><b>idx_role_key</b> (role_key) - 角色权限查询、唯一性约束</li>
 * <li><b>idx_status</b> (status) - 角色状态筛选</li>
 * <li><b>idx_create_time</b> (create_time) - 按创建时间排序</li>
 * </ul>
 * <p>
 * 性能优化建议：
 * <ul>
 * <li>role_key 应设置为 UNIQUE 索引，避免角色键重复</li>
 * <li>如果经常按状态查询激活角色，可考虑 (status, role_key) 联合索引</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Data
@TableName("sys_role")
public class RoleEntity {

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
     * 角色排序 (数字越小越靠前)
     */
    private Integer roleSort;

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
