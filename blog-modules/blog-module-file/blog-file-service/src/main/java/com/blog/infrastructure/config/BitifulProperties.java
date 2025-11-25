package com.blog.infrastructure.config;

import com.blog.infrastructure.constant.FileStorageConstants;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

/**
 * Bitiful 配置属性
 * <p>
 * 使用 Spring Boot 3 的 @ConfigurationProperties 绑定 YAML 配置。
 * 字段使用 @NotBlank 强制校验，防止空值注入。
 * 后期微服务拆分时，可通过 Kubernetes ConfigMap/Secret 注入。
 * </p>
 *
 * @author liusxml
 * @since 1.0-SNAPSHOT
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "bitiful")
public class BitifulProperties {

    /**
     * Bitiful S3 兼容 endpoint
     * <p>
     * 默认：<a href="https://s3.bitiful.net/">...</a>
     * </p>
     */
    @NotBlank(message = "bitiful.endpoint 不能为空")
    private String endpoint = "https://s3.bitiful.net/";

    /**
     * 子账户 Access Key（控制台创建，最小权限：PUT/GET/DELETE）
     */
    @NotBlank(message = "bitiful.access-key 不能为空")
    private String accessKey;

    /**
     * 子账户 Secret Key
     */
    @NotBlank(message = "bitiful.secret-key 不能为空")
    private String secretKey;

    /**
     * Bucket 名称（控制台创建，建议开启公开读用于图片直链）
     */
    @NotBlank(message = "bitiful.bucket 不能为空")
    private String bucket = "blog-files";

    /**
     * 区域（Bitiful S3 兼容默认 cn-east-1）
     */
    @NotBlank(message = "bitiful.region 不能为空")
    private String region = "cn-east-1";

    /**
     * 最大文件大小限制（字节）
     * <p>
     * 默认 10MB，可通过配置文件覆盖。
     * 最小值为 1KB。
     * </p>
     */
    @Min(value = FileStorageConstants.MIN_FILE_SIZE, message = "文件大小限制不能小于 1KB")
    private Long maxFileSize = FileStorageConstants.DEFAULT_MAX_FILE_SIZE;

    /**
     * 允许上传的文件扩展名集合
     * <p>
     * 默认包含常见图片和文档格式。
     * 配置示例：
     * 
     * <pre>
     * bitiful:
     *   allowed-extensions:
     *     - jpg
     *     - png
     *     - pdf
     * </pre>
     * </p>
     */
    @NotEmpty(message = "允许的文件扩展名列表不能为空")
    private Set<String> allowedExtensions = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf", "docx");
}