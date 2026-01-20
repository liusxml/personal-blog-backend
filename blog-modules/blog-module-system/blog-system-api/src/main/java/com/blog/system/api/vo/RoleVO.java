package com.blog.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色信息 VO
 *
 * @author liusxml
 * @since 1.0.0
 */
@Data
@Schema(description = "角色信息VO")
public class RoleVO implements Serializable {

    @Schema(description = "角色ID")
    private String id; // Long序列化为String，避免精度丢失

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色权限字符串")
    private String roleKey;

    @Schema(description = "状态：1-正常，0-停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
