package com.blog.system.controller;

import com.blog.common.model.Result;
import com.blog.system.api.dto.RoleDTO;
import com.blog.system.api.vo.RoleVO;
import com.blog.system.service.IRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * 角色管理控制器
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // 所有接口都需要管理员权限
@Tag(name = "角色管理", description = "角色CRUD和用户角色关联管理接口")
public class RoleController {

    private final IRoleService roleService;

    /**
     * 创建角色
     */
    @PostMapping
    @Operation(summary = "创建角色", description = "创建新的系统角色")
    public Result<Long> createRole(@Valid @RequestBody RoleDTO roleDTO) {
        Long roleId = (Long) roleService.saveByDto(roleDTO);
        return Result.success(roleId);
    }

    /**
     * 获取角色详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取角色详情", description = "根据ID获取角色详细信息")
    public Result<RoleVO> getRoleById(@PathVariable Long id) {
        Optional<RoleVO> roleVO = roleService.getVoById(id);
        return roleVO.map(Result::success)
                .orElseGet(() -> Result.error(404, "角色不存在"));
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新角色", description = "更新角色信息")
    public Result<Void> updateRole(@PathVariable Long id, @Valid @RequestBody RoleDTO roleDTO) {
        roleDTO.setId(id);
        roleService.updateByDto(roleDTO);
        return Result.success();
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色", description = "删除指定角色")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.removeById(id);
        return Result.success();
    }

    /**
     * 为用户分配角色
     */
    @PostMapping("/{roleId}/users/{userId}")
    @Operation(summary = "分配角色", description = "为指定用户分配角色")
    public Result<Void> assignRoleToUser(
            @PathVariable Long roleId,
            @PathVariable Long userId) {
        boolean success = roleService.assignRoleToUser(userId, roleId);
        return success ? Result.success() : Result.error(500, "分配角色失败");
    }

    /**
     * 移除用户角色
     */
    @DeleteMapping("/{roleId}/users/{userId}")
    @Operation(summary = "移除角色", description = "移除用户的指定角色")
    public Result<Void> removeRoleFromUser(
            @PathVariable Long roleId,
            @PathVariable Long userId) {
        boolean success = roleService.removeRoleFromUser(userId, roleId);
        return success ? Result.success() : Result.error(500, "移除角色失败");
    }
}
