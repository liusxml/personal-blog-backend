package com.blog.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件 VO
 * 
 * <p>
 * 用于返回给前端的文件信息。
 * 实现 Serializable 接口。
 * </p>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Data
@Schema(description = "文件视图对象")
public class FileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文件ID")
    private Long id;

    @Schema(description = "存储键")
    private String fileKey;

    @Schema(description = "存储类型")
    private String storageType;

    @Schema(description = "Bucket名称")
    private String bucket;

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "MIME类型")
    private String contentType;

    @Schema(description = "扩展名")
    private String extension;

    @Schema(description = "文件分类")
    private String fileCategory;

    @Schema(description = "图片宽度")
    private Integer imageWidth;

    @Schema(description = "图片高度")
    private Integer imageHeight;

    @Schema(description = "引用类型")
    private String refType;

    @Schema(description = "引用对象ID")
    private Long refId;

    @Schema(description = "CDN URL")
    private String cdnUrl;

    @Schema(description = "访问策略")
    private String accessPolicy;

    @Schema(description = "上传状态：0=待上传,1=已完成,2=失败")
    private Integer uploadStatus;

    @Schema(description = "下载次数")
    private Integer downloadCount;

    @Schema(description = "查看次数")
    private Integer viewCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人ID")
    private Long createBy;
}
