package com.blog.dto;

import com.blog.common.base.Identifiable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件 DTO
 * 
 * <p>
 * 遵循项目规范：
 * <ul>
 * <li>实现 Serializable 接口</li>
 * <li>实现 Identifiable&lt;Long&gt; 接口</li>
 * <li>使用 @Schema 注解（OpenAPI文档）</li>
 * </ul>
 * </p>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Data
@Schema(description = "文件DTO")
public class FileDTO implements Serializable, Identifiable<Long> {

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

    @Schema(description = "MD5值")
    private String md5;

    @Schema(description = "文件分类：IMAGE、VIDEO、DOCUMENT、AUDIO、OTHER")
    private String fileCategory;

    @Schema(description = "图片宽度")
    private Integer imageWidth;

    @Schema(description = "图片高度")
    private Integer imageHeight;

    @Schema(description = "引用类型：ARTICLE、COMMENT、AVATAR、ATTACHMENT")
    private String refType;

    @Schema(description = "引用对象ID")
    private Long refId;

    @Schema(description = "CDN URL（PUBLIC文件）")
    private String cdnUrl;

    @Schema(description = "访问策略")
    private String accessPolicy;

    @Schema(description = "上传状态：0=待上传,1=已完成,2=失败")
    private Integer uploadStatus;

    @Schema(description = "下载次数")
    private Integer downloadCount;

    @Schema(description = "查看次数")
    private Integer viewCount;

    @Schema(description = "CoreIX处理参数")
    private String processParams;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
