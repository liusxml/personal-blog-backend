package com.blog.system.controller;

import com.blog.common.model.Result;
import com.blog.system.api.dto.LoginDTO;
import com.blog.system.api.dto.RegisterDTO;
import com.blog.system.api.vo.LoginVO;
import com.blog.system.api.vo.UserVO;
import com.blog.system.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户注册、登录、登出等认证相关接口")
public class AuthController {

    private final IUserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户账号")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO registerDTO) {
        log.info("收到注册请求: username={}, email={}", registerDTO.getUsername(), registerDTO.getEmail());
        UserVO userVO = userService.register(registerDTO);
        return Result.success(userVO);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名/邮箱和密码登录，返回JWT Token")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        log.info("收到登录请求: username={}", loginDTO.getUsername());
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success(loginVO);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "登出当前用户（客户端需删除Token）")
    public Result<Void> logout() {
        // JWT 是无状态的，登出由客户端删除 Token 实现
        // 这里可以添加黑名单机制或记录登出日志
        log.info("用户登出");
        return Result.success();
    }
}
