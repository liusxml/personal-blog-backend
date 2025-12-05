package com.blog.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录响应 VO
 *
 * @author liusxml
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录响应VO")
public class LoginVO implements Serializable {

    @Schema(description = "JWT Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Token类型", example = "Bearer")
    private String tokenType;

    @Schema(description = "Token有效期（秒）", example = "7200")
    private Long expiresIn;

    @Schema(description = "用户信息")
    private UserVO user;
}
