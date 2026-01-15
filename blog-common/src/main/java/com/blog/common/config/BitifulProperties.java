package com.blog.common.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

/**
 * Bitiful 对象存储配置属性
 *
 * <p>
 * 使用 Spring Boot @ConfigurationProperties 自动绑定 YAML 配置。
 * 支持 JSR-303 验证，确保配置完整性。
 * </p>
 *
 * <p>
 * <strong>配置示例：</strong>
 *
 * <pre>
 * oss:
 *   bitiful:
 *     endpoint: https://s3.bitiful.net
 *     access-key: ${BITIFUL_ACCESS_KEY}
 *     secret-key: ${BITIFUL_SECRET_KEY}
 *     bucket: blog-files
 * </pre>
 * </p>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "oss.bitiful")
public class BitifulProperties {

    /**
     * Bitiful S3 兼容 Endpoint
     *
     * <p>
     * 默认：https://s3.bitiful.net
     * </p>
     */
    @NotBlank(message = "bitiful.endpoint 不能为空")
    private String endpoint = "https://s3.bitiful.net";

    /**
     * 子账户 Access Key
     *
     * <p>
     * <strong>安全提醒：</strong>生产环境必须通过环境变量注入，不要硬编码！
     * </p>
     */
    @NotBlank(message = "bitiful.access-key 不能为空")
    private String accessKey;

    /**
     * 子账户 Secret Key
     *
     * <p>
     * <strong>安全提醒：</strong>生产环境必须通过环境变量注入，不要硬编码！
     * </p>
     */
    @NotBlank(message = "bitiful.secret-key 不能为空")
    private String secretKey;

    /**
     * Bucket 名称
     *
     * <p>
     * 建议开启公开读用于图片直链。
     * </p>
     */
    @NotBlank(message = "bitiful.bucket 不能为空")
    private String bucket = "blog-files";

    /**
     * 区域
     *
     * <p>
     * Bitiful S3 兼容默认区域：cn-east-1
     * </p>
     */
    @NotBlank(message = "bitiful.region 不能为空")
    private String region = "cn-east-1";

    /**
     * 最大文件大小限制（字节）
     *
     * <p>
     * 默认 10MB，可通过配置文件覆盖。
     * 最小值为 1KB。
     * </p>
     */
    @Min(value = 1024, message = "文件大小限制不能小于 1KB")
    private Long maxFileSize = 10 * 1024 * 1024L; // 10MB

    /**
     * 允许上传的文件扩展名集合
     *
     * <p>
     * 默认包含常见图片和文档格式。
     * 配置示例：
     *
     * <pre>
     * oss:
     *   bitiful:
     *     allowed-extensions:
     *       - jpg
     *       - png
     *       - pdf
     * </pre>
     * </p>
     */
    @NotEmpty(message = "允许的文件扩展名列表不能为空")
    private Set<String> allowedExtensions = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "pdf", "docx");
}
