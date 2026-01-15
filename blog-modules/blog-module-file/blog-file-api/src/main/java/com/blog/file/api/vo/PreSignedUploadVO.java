package com.blog.file.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 预签名上传 URL 响应 VO
 *
 * <p>
 * 返回给前端的预签名 URL 信息，用于直传 Bitiful S4。
 * </p>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Data
@Schema(description = "预签名上传URL响应")
public class PreSignedUploadVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "预签名PUT URL", example = "https://s3.bitiful.net/blog-files/uploads/2025/12/xxx.jpg?X-Amz-...")
    private String uploadUrl;

    @Schema(description = "服务端生成的存储键", example = "uploads/2025/12/13/abc123.jpg")
    private String fileKey;

    @Schema(description = "预分配的文件ID", example = "1234567890")
    private Long fileId;

    @Schema(description = "URL有效期（秒）", example = "1800")
    private Integer expireSeconds;

    @Schema(description = "上传完成回调URL", example = "/api/v1/files/1234567890/confirm")
    private String callbackUrl;

    @Schema(description = "是否为秒传（文件已存在）", example = "true")
    private Boolean instant;
}
