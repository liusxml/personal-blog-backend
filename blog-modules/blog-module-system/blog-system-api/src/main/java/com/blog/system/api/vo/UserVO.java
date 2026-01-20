package com.blog.system.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息 VO
 *
 * @author liusxml
 * @since 1.0.0
 */
@Data
@Schema(description = "用户信息VO")
public class UserVO implements Serializable {

    @Schema(description = "用户ID")
    private String id; // Long序列化为String，避免精度丢失

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "状态：1-正常，0-禁用")
    private Integer status;

    @Schema(description = "角色列表")
    private List<String> roles;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "备注")
    private String remark;
}
