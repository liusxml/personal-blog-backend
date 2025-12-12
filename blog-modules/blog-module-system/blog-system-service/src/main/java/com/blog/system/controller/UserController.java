package com.blog.system.controller;

import com.blog.common.model.Result;
import com.blog.common.utils.SecurityUtils;
import com.blog.system.api.dto.UserDTO;
import com.blog.system.api.vo.UserVO;
import com.blog.system.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * 用户管理控制器
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户信息查询、更新等管理接口")
public class UserController {

    private final IUserService userService;

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    public Result<UserVO> getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        Optional<UserVO> userVO = userService.getVoById(userId);
        return userVO.map(Result::success)
                .orElseGet(() -> Result.error(404, "用户不存在"));
    }

    /**
     * 更新当前用户信息
     */
    @PutMapping("/me")
    @Operation(summary = "更新个人资料", description = "更新当前登录用户的个人信息")
    public Result<Void> updateCurrentUser(@Valid @RequestBody UserDTO userDTO) {
        Long userId = SecurityUtils.getCurrentUserId();
        userDTO.setId(userId); // 确保只能更新自己的信息
        userService.updateByDto(userDTO);
        return Result.success();
    }

    /**
     * 根据ID获取用户信息（需要管理员权限）
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "获取用户信息", description = "根据ID获取用户详细信息（需要管理员权限）")
    public Result<UserVO> getUserById(@PathVariable("id") Long id) {
        Optional<UserVO> userVO = userService.getVoById(id);
        return userVO.map(Result::success)
                .orElseGet(() -> Result.error(404, "用户不存在"));
    }

    /**
     * 更新用户信息（需要管理员权限）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新用户信息", description = "更新指定用户的信息（需要管理员权限）")
    public Result<Boolean> updateUser(@PathVariable("id") Long id, @RequestBody UserDTO userDTO) {
        userDTO.setId(id);
        userService.updateByDto(userDTO);
        return Result.success();
    }

    /**
     * 删除用户（需要管理员权限）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除用户", description = "删除指定用户（需要管理员权限）")
    public Result<Boolean> deleteUser(@PathVariable("id") Long id) {
        userService.removeById(id);
        return Result.success();
    }
}
