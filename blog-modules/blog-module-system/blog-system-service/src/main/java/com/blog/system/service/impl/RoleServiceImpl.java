package com.blog.system.service.impl;

import com.blog.common.base.BaseServiceImpl;
import com.blog.system.api.dto.RoleDTO;
import com.blog.system.api.vo.RoleVO;
import com.blog.system.converter.RoleConverter;
import com.blog.system.entity.SysRole;
import com.blog.system.mapper.RoleMapper;
import com.blog.system.service.IRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色服务实现
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@Service
public class RoleServiceImpl extends BaseServiceImpl<RoleMapper, SysRole, RoleVO, RoleDTO, RoleConverter>
        implements IRoleService {

    private final RoleMapper roleMapper;

    public RoleServiceImpl(RoleConverter roleConverter, RoleMapper roleMapper) {
        super(roleConverter);
        this.roleMapper = roleMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRoleToUser(Long userId, Long roleId) {
        log.info("为用户分配角色: userId={}, roleId={}", userId, roleId);
        int result = roleMapper.assignRoleToUser(userId, roleId);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeRoleFromUser(Long userId, Long roleId) {
        log.info("移除用户角色: userId={}, roleId={}", userId, roleId);
        int result = roleMapper.removeRoleFromUser(userId, roleId);
        return result > 0;
    }
}
