package com.blog.dto;

import com.blog.common.base.Identifiable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 预签名 URL 请求 DTO
 * 
 * <p>
 * 用于前端请求预签名上传 URL（官方推荐方案）。
 * 遵循项目规范，实现 Serializable 和 Identifiable 接口。
 * </p>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Data
@Schema(description = "预签名上传URL请求")
public class PreSignedUrlRequest implements Serializable, Identifiable<Long> {

    private static final long serialVersionUID = 1L;

    @Schema(description = "ID（可选）")
    private Long id;

    @NotBlank(message = "文件名不能为空")
    @Schema(description = "原始文件名", example = "avatar.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;

    @NotBlank(message = "Content-Type不能为空")
    @Schema(description = "MIME类型", example = "image/jpeg", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contentType;

    @NotNull(message = "文件大小不能为空")
    @Min(value = 1024, message = "文件大小不能小于1KB")
    @Max(value = 10485760, message = "文件大小不能超过10MB")
    @Schema(description = "文件大小（字节）", example = "1024000", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fileSize;

    @Min(1)
    @Max(60)
    @Schema(description = "过期时间（分钟）", example = "30", defaultValue = "30")
    private Integer expireMinutes = 30;

    @Schema(description = "MD5值（可选，用于秒传）", example = "d41d8cd98f00b204e9800998ecf8427e")
    private String md5;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
