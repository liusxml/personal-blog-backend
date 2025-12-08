package com.blog.system.service.impl;

import com.blog.common.base.BaseServiceImpl;
import com.blog.common.constants.CacheKeys;
import com.blog.common.exception.BusinessException;
import com.blog.common.exception.SystemErrorCode;
import com.blog.common.utils.RedisUtils;
import com.blog.system.api.dto.RoleDTO;
import com.blog.system.api.vo.RoleVO;
import com.blog.system.converter.RoleConverter;
import com.blog.system.entity.SysRole;
import com.blog.system.mapper.RoleMapper;
import com.blog.system.service.IRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

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
    private final RedisUtils redisUtils;

    public RoleServiceImpl(RoleConverter roleConverter,
                           RoleMapper roleMapper,
                           RedisUtils redisUtils) {
        super(roleConverter);
        this.roleMapper = roleMapper;
        this.redisUtils = redisUtils;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user:roles", key = "#userId")
    public boolean assignRoleToUser(Long userId, Long roleId) {
        log.info("分配角色: userId={}, roleId={}", userId, roleId);
        roleMapper.assignRoleToUser(userId, roleId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user:roles", key = "#userId")
    public boolean removeRoleFromUser(Long userId, Long roleId) {
        log.info("移除角色: userId={}, roleId={}", userId, roleId);
        roleMapper.removeRoleFromUser(userId, roleId);
        return true;
    }

    /**
     * 删除角色（重写以添加业务校验和清理关联数据）
     * <p>
     * 删除流程：
     * 1. 先查询角色（获取 version 用于乐观锁）
     * 2. 检查角色是否被用户使用
     * 3. 如果正在使用，抛出业务异常
     * 4. 删除所有角色-用户关联关系
     * 5. 删除角色本身（逻辑删除，需要 version 字段）
     * 6. 清除所有用户角色缓存，确保数据一致性
     *
     * @param id 角色ID
     * @return true-删除成功，false-删除失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "user:roles", allEntries = true)
    public boolean removeById(Serializable id) {
        Long roleId = (Long) id;
        log.info("开始删除角色: roleId={}", roleId);

        // 1. 先查询角色（获取完整实体，包括 version 字段）
        SysRole role = this.getById(roleId);
        if (role == null) {
            log.warn("角色不存在: roleId={}", roleId);
            return false;
        }

        // 2. 检查角色是否被使用
        Integer userCount = roleMapper.countUsersByRoleId(roleId);
        if (userCount != null && userCount > 0) {
            log.warn("角色删除失败，该角色正在被使用: roleId={}, userCount={}", roleId, userCount);
            throw new BusinessException(
                    SystemErrorCode.ROLE_IN_USE,
                    "该角色正在被 " + userCount + " 个用户使用，无法删除");
        }

        // 3. 删除所有角色-用户关联关系（清理孤儿数据）
        int deletedRelations = roleMapper.deleteAllRoleUserRelations(roleId);
        log.info("清理角色关联关系: roleId={}, 删除关系数={}", roleId, deletedRelations);

        // 4. 删除角色本身（MyBatis-Plus 会执行逻辑删除，需要 version 进行乐观锁检查）
        boolean success = super.removeById(role);

        // 5. 清除角色详情缓存（使用 RedisUtils 工具类）
        if (success) {
            String roleDetailKey = CacheKeys.roleDetailKey(roleId);
            redisUtils.delete(roleDetailKey);
            log.info("已清除角色详情缓存: key={}", roleDetailKey);
        }

        if (success) {
            log.info("角色删除成功，已清除所有用户角色缓存: roleId={}", roleId);
        } else {
            log.error("角色删除失败（可能是版本冲突）: roleId={}, version={}", roleId, role.getVersion());
        }

        return success;
    }
}
