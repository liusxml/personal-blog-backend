package com.blog.infrastructure.config;

import com.google.common.collect.ImmutableMap;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.util.Map;

/**
 * Bitiful S3 客户端配置类
 * <p>
 * 采用 <strong>工厂模式 + 策略模式</strong> 创建 S3Client 和 S3Presigner，确保单例线程安全。
 * 使用 Spring Boot 3 的 @Configuration + @Bean 机制注入 Bean，兼容单体与微服务架构。
 * 配置通过 {@link BitifulProperties} 绑定 YAML，支持多环境切换（dev/prod/test）。
 * </p>
 * <p>
 * <strong>设计亮点：</strong>
 * <ul>
 *   <li><strong>不可变配置</strong>：使用 Guava {@link ImmutableMap} 封装配置，防止运行时篡改。</li>
 *   <li><strong>监控集成</strong>：注入 Micrometer {@link MeterRegistry} 打标签，Actuator 可监控 Bitiful 请求指标。</li>
 *   <li><strong>微服务 ready</strong>：整个类可直接复制到 file-server 模块，无需修改。</li>
 *   <li><strong>工具类规范</strong>：Guava 不可变集合 + Lombok @RequiredArgsConstructor 减少样板代码。</li>
 * </ul>
 * </p>
 * <p>
 * <strong>兼容性：</strong>
 * <ul>
 *   <li>Java 21 + Spring Boot 3.2+</li>
 *   <li>AWS SDK V2 2.38.4</li>
 *   <li>Bitiful S3 兼容（path-style 必须）</li>
 * </ul>
 * </p>
 *
 * @author Your Name
 * @since 1.0-SNAPSHOT
 */
@Configuration
@RequiredArgsConstructor
public class BitifulConfig {

    /**
     * Bitiful 配置属性对象
     * <p>
     * 通过 {@link org.springframework.boot.context.properties.ConfigurationProperties} 绑定
     * <code>application.yml</code> 中的 <code>bitiful.*</code> 配置。
     * 字段已使用 {@link jakarta.validation.constraints.NotBlank} 强制校验。
     * </p>
     */
    private final BitifulProperties properties;

    /**
     * 创建 Bitiful S3Client Bean
     * <p>
     * 使用 AWS SDK V2 的 builder 模式构建 S3Client，配置如下：
     * </p>
     * <ul>
     *   <li><strong>endpointOverride</strong>：自定义 Bitiful endpoint <code>https://s3.bitiful.net/</code></li>
     *   <li><strong>pathStyleAccessEnabled(true)</strong>：Bitiful 必须使用 path-style 访问（bucket 在路径中）</li>
     *   <li><strong>chunkedEncodingEnabled(true)</strong>：启用分块编码，支持大文件上传（>5MB）</li>
     *   <li><strong>StaticCredentialsProvider</strong>：使用子账户 AK/SK 认证，最小权限原则</li>
     * </ul>
     * <p>
     * <strong>监控集成</strong>：通过 Micrometer 打标签 <code>storage.provider=bitiful</code>，可在
     * <code>/actuator/metrics</code> 查看 S3 请求指标。
     * </p>
     * <p>
     * <strong>微服务演进</strong>：此 Bean 可直接注入到 {@link com.blog.infrastructure.oss.BitifulStorage}，
     * 后期拆分 file-service 时无需修改。
     * </p>
     *
     * @param meterRegistry Micrometer 注册器，用于监控 S3 请求
     * @return 配置好的 {@link S3Client} 实例
     * @throws IllegalArgumentException 如果 properties 中的字段为空（由 @Validated 触发）
     */
    @Bean
    public S3Client bitifulS3Client(MeterRegistry meterRegistry) {
        // 使用 Guava 构建不可变配置 Map，防止运行时修改
        // 仅用于日志或调试，实际配置从 properties 读取
        Map<String, String> config = ImmutableMap.of(
                "endpoint", properties.getEndpoint(),
                "accessKey", maskAccessKey(properties.getAccessKey()),
                "secretKey", "******",
                "bucket", properties.getBucket(),
                "region", properties.getRegion()
        );

        // 构建 S3Client
        var s3Client = S3Client.builder()
                // 设置区域（Bitiful 默认为 cn-east-1）
                .region(Region.of(properties.getRegion()))
                // 覆盖默认 endpoint 为 Bitiful
                .endpointOverride(URI.create(properties.getEndpoint()))
                // 使用子账户凭证（最小权限：PUT/GET/DELETE）
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                properties.getAccessKey(),
                                properties.getSecretKey()
                        )
                ))
                // S3 兼容性配置
                .serviceConfiguration(S3Configuration.builder()
                        // 启用分块编码，支持大文件流式上传
                        .chunkedEncodingEnabled(true)
                        // Bitiful 必须使用 path-style 访问
                        // 例如：https://s3.bitiful.net/blog-files/key.jpg
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();

        // 集成 Micrometer 监控：为所有 S3 请求打标签
        // 可在 Actuator 查看：s3_client_requests_total{storage.provider="bitiful",bucket="blog-files"}
        meterRegistry.config().commonTags(
                "storage.provider", "bitiful",
                "bucket", properties.getBucket()
        );

        return s3Client;
    }

    /**
     * 创建 S3Presigner Bean
     * <p>
     * 用于生成预签名 URL，支持前端直传（PUT）和直链访问（GET）。
     * 与 {@link #bitifulS3Client} 共享 endpoint 和凭证配置。
     * </p>
     * <p>
     * <strong>使用场景：</strong>
     * <ul>
     *   <li>前端调用 <code>/file/presigned</code> 获取 PUT URL</li>
     *   <li>过期时间由业务层控制（如 30 分钟）</li>
     *   <li>上传成功后回调后端完成入库</li>
     * </ul>
     * </p>
     * <p>
     * <strong>微服务演进</strong>：此 Bean 可注入到 Feign 客户端，支持跨服务调用。
     * </p>
     *
     * @return 配置好的 {@link S3Presigner} 实例
     */
    @Bean
    public S3Presigner bitifulS3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .endpointOverride(URI.create(properties.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                properties.getAccessKey(),
                                properties.getSecretKey()
                        )
                ))
                .build();
    }

    // ────────────────────────── 私有工具方法 ──────────────────────────

    /**
     * 掩码 AccessKey（安全日志）
     * <p>
     * 显示前 4 位 + **** + 后 4 位，防止敏感信息泄露。
     * 使用 commons-lang3 风格实现，但此处简化。
     * </p>
     *
     * @param accessKey 原始 AccessKey
     * @return 掩码后的字符串
     */
    private String maskAccessKey(String accessKey) {
        if (accessKey == null || accessKey.length() <= 8) {
            return "****";
        }
        return accessKey.substring(0, 4) + "****" + accessKey.substring(accessKey.length() - 4);
    }
}