package com.blog.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全相关的自定义配置属性。
 * 通过 @ConfigurationProperties 注解将 application.yml 中的 app.security
 * 前缀的配置绑定到该类的字段上。
 *
 * @author liusxml
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /**
     * URL 白名单配置 (对应 app.security.permit-all-urls)
     */
    private List<String> permitAllUrls = new ArrayList<>();

    /**
     * JWT 签名密钥
     */
    private String jwtSecret = "default-secret-key-change-in-production-at-least-256-bits-long";

    /**
     * Token 过期时间，默认 2 小时
     */
    private Long jwtExpiration = 7200000L;

}
