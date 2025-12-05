package com.blog.system.api.dto;

import com.blog.common.base.Identifiable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息 DTO
 *
 * @author liusxml
 * @since 1.0.0
 */
@Data
@Schema(description = "用户信息DTO")
public class UserDTO implements Identifiable<Long>, Serializable {

    @Schema(description = "用户ID", example = "1234567890")
    private Long id;

    @Schema(description = "用户名", example = "zhangsan")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 64, message = "用户名长度为3-64位")
    private String username;

    @Schema(description = "昵称", example = "张三")
    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称不能超过64位")
    private String nickname;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    @Schema(description = "状态：1-正常，0-禁用", example = "1")
    private Integer status;

    @Schema(description = "角色列表", example = "[\"ROLE_USER\", \"ROLE_AUTHOR\"]")
    private List<String> roles;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "备注")
    private String remark;
}
