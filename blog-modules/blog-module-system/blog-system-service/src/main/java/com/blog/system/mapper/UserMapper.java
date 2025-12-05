package com.blog.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.system.entity.SysRole;
import com.blog.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户 Mapper
 *
 * @author liusxml
 * @since 1.0.0
 */
@Mapper
public interface UserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户ID查询用户的角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @Select("SELECT r.* FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.is_deleted = 0")
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);
}
