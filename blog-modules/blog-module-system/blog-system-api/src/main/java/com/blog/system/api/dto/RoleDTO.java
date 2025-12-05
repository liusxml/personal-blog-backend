package com.blog.system.api.dto;

import com.blog.common.base.Identifiable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色信息 DTO
 *
 * @author liusxml
 * @since 1.0.0
 */
@Data
@Schema(description = "角色信息DTO")
public class RoleDTO implements Identifiable<Long>, Serializable {

    @Schema(description = "角色ID", example = "1")
    private Long id;

    @Schema(description = "角色名称", example = "管理员", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    @Schema(description = "角色权限字符串（大写字母和下划线）", example = "ADMIN", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "角色权限字符串不能为空")
    @Pattern(regexp = "^[A-Z_]+$", message = "角色KEY只能包含大写字母和下划线")
    private String roleKey;

    @Schema(description = "状态：1-正常，0-停用", example = "1")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
