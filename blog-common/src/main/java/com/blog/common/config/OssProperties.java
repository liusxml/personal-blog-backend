package com.blog.common.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * OSS 统一配置属性
 *
 * <p>
 * 控制使用哪种 OSS 类型（BITIFUL、ALIYUN、MINIO 等）。
 * 支持运行时切换存储策略。
 * </p>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    /**
     * OSS 类型
     *
     * <p>
     * 可选值：
     * <ul>
     * <li>BITIFUL - 缤纷云 S4 对象存储</li>
     * <li>ALIYUN - 阿里云 OSS</li>
     * <li>MINIO - MinIO 自建存储</li>
     * <li>LOCAL - 本地文件系统</li>
     * </ul>
     * </p>
     */
    @NotBlank(message = "OSS 类型不能为空")
    private String type = "BITIFUL";
}
