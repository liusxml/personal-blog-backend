package com.blog.system.service;

import com.blog.common.base.IBaseService;
import com.blog.system.api.dto.RoleDTO;
import com.blog.system.api.vo.RoleVO;
import com.blog.system.domain.entity.RoleEntity;

/**
 * 角色服务接口
 *
 * @author liusxml
 * @since 1.0.0
 */
public interface IRoleService extends IBaseService<RoleEntity, RoleVO, RoleDTO> {

    /**
     * 为用户分配角色
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 是否成功
     */
    boolean assignRoleToUser(Long userId, Long roleId);

    /**
     * 移除用户的角色
     *
     * @param userId 用户ID
     * @param roleId 角色ID
     * @return 是否成功
     */
    boolean removeRoleFromUser(Long userId, Long roleId);
}
