package com.blog.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.system.entity.SysRole;
import com.blog.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 用户 Mapper
 * <p>
 * SQL 实现位于：resources/mapper/UserMapper.xml
 *
 * @author liusxml
 * @since 1.0.0
 */
@Mapper
public interface UserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户ID查询用户的角色列表
     * <p>
     * 优化说明：
     * <ul>
     * <li>使用 XML 定义 SQL，便于维护复杂查询</li>
     * <li>添加了 role.status = 1 条件，只查询启用的角色</li>
     * <li>添加了排序，按 role_sort 升序</li>
     * <li>使用 resultMap 映射，避免字段重复定义</li>
     * </ul>
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 根据用户名查询用户
     * <p>
     * 用于登录认证，利用 idx_username 索引
     *
     * @param username 用户名
     * @return 用户实体，不存在返回 null
     */
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     * <p>
     * 用于登录认证，利用 idx_email 索引
     *
     * @param email 邮箱
     * @return 用户实体，不存在返回 null
     */
    SysUser selectByEmail(@Param("email") String email);

    /**
     * 批量查询用户（用于 RemoteUserService）
     * <p>
     * 限制最大查询数量为 100，防止慢查询
     *
     * @param ids 用户ID列表
     * @return 用户列表
     */
    List<SysUser> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 批量根据账号查询用户ID
     *
     * @param usernames 用户名集合
     * @return 用户ID列表
     */
    @Select("<script>" +
            "SELECT id FROM sys_user " +
            "WHERE username IN " +
            "<foreach collection='usernames' item='name' open='(' separator=',' close=')'>" +
            "#{name}" +
            "</foreach>" +
            " AND is_deleted = 0" +
            "</script>")
    List<Long> selectUserIdsByUsernames(@Param("usernames") Collection<String> usernames);

    /**
     * 检查用户名是否存在
     * <p>
     * 用于注册验证，只查询计数不返回完整数据
     *
     * @param username 用户名
     * @return true-存在，false-不存在
     */
    Boolean existsByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否存在
     * <p>
     * 用于注册验证，只查询计数不返回完整数据
     *
     * @param email 邮箱
     * @return true-存在，false-不存在
     */
    Boolean existsByEmail(@Param("email") String email);
}
