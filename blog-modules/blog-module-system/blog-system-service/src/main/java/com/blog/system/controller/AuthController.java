package com.blog.system.controller;

import com.blog.common.constants.CacheKeys;
import com.blog.common.model.Result;
import com.blog.common.utils.SecurityUtils;
import com.blog.system.api.dto.LoginDTO;
import com.blog.system.api.dto.RegisterDTO;
import com.blog.system.api.vo.LoginVO;
import com.blog.system.api.vo.UserVO;
import com.blog.system.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

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
    private final StringRedisTemplate stringRedisTemplate;

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

    /**
     * 签发 MCP 客户端接入专用长效 Bearer Token（仅限管理员）
     * <p>
     * 通过 {@link SecurityUtils#getCurrentUserId()} 获取当前已认证的管理员身份，
     * 签发指定有效天数的长效 JWT Token，供 Antigravity / Cursor / Claude Desktop 等 MCP 客户端配置使用。
     *
     * @param expirationDays Token 有效天数（1~365 天，默认 30 天）
     * @return 包含 Bearer Token 及过期时间的响应对象
     */
    @PostMapping("/mcp-token")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "签发 MCP 专用 Token", description = "为当前登录管理员签发用于 MCP 客户端接入的长效 Bearer Token")
    public Result<LoginVO> generateMcpToken(
            @Parameter(description = "Token 有效天数（1~365 天，默认 30 天）", example = "30")
            @RequestParam(value = "expirationDays", defaultValue = "30") Integer expirationDays) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("管理员请求签发 MCP Token: userId={}, expirationDays={}", currentUserId, expirationDays);
        LoginVO mcpTokenVO = userService.generateMcpToken(currentUserId, expirationDays);
        return Result.success(mcpTokenVO);
    }

    /**
     * 吊销当前管理员已签发的所有 MCP 专用长效 Token（仅限管理员）
     * <p>
     * 将当前时间戳（向上取整 +1s 覆盖秒级 iat 截断）写入 Redis 键 {@code mcp:token:revoked_before:{userId}}，
     * 使得此前签发的所有 MCP Token 立即失效。
     *
     * @return 空成功响应
     */
    @DeleteMapping("/mcp-token")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "吊销 MCP 专用 Token", description = "立即作废当前登录管理员此前签发的所有 MCP 客户端长效 Token")
    public Result<Void> revokeMcpToken() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        long revokedBeforeMillis = System.currentTimeMillis() + 1000L;
        stringRedisTemplate.opsForValue().set(
                CacheKeys.mcpTokenRevokedBeforeKey(currentUserId),
                String.valueOf(revokedBeforeMillis),
                Duration.ofDays(365));
        log.info("管理员已吊销所有历史 MCP Token: userId={}, revokedBeforeMillis={}", currentUserId, revokedBeforeMillis);
        return Result.success();
    }
}
