package com.blog.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.system.domain.entity.RoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色 Mapper
 * <p>
 * SQL 实现位于：resources/mapper/RoleMapper.xml
 *
 * @author liusxml
 * @since 1.0.0
 */
@Mapper
public interface RoleMapper extends BaseMapper<RoleEntity> {

    /**
     * 为用户分配角色
     * <p>
     * 优化说明：使用 ON DUPLICATE KEY UPDATE 防止重复插入
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 影响行数
     */
    int assignRoleToUser(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 移除用户角色
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 影响行数
     */
    int removeRoleFromUser(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 根据角色KEY查询角色
     * <p>
     * 用于注册时分配默认角色，利用 idx_role_key 唯一索引
     *
     * @param roleKey 角色KEY（如 "USER", "ADMIN"）
     * @return 角色实体，不存在返回 null
     */
    RoleEntity selectByRoleKey(@Param("roleKey") String roleKey);

    /**
     * 查询所有启用的角色
     * <p>
     * 用于角色列表展示，按 role_sort 排序
     *
     * @return 启用的角色列表
     */
    List<RoleEntity> selectAllActive();

    /**
     * 检查角色KEY是否存在
     * <p>
     * 用于创建角色验证，只查询计数
     *
     * @param roleKey 角色KEY
     * @return true-存在，false-不存在
     */
    Boolean existsByRoleKey(@Param("roleKey") String roleKey);

    /**
     * 统计拥有该角色的用户数量
     * <p>
     * 用于删除角色前的检查，防止删除仍在使用的角色
     *
     * @param roleId 角色ID
     * @return 用户数量
     */
    Integer countUsersByRoleId(@Param("roleId") Long roleId);

    /**
     * 删除角色的所有用户关联关系
     * <p>
     * 用于删除角色前清理关联表数据
     *
     * @param roleId 角色ID
     * @return 删除的记录数
     */
    int deleteAllRoleUserRelations(@Param("roleId") Long roleId);

    /**
     * 批量查询角色
     *
     * @param ids 角色ID列表
     * @return 角色列表
     */
    List<RoleEntity> selectByIds(@Param("ids") List<Long> ids);
}
