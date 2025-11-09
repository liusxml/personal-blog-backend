package com.blog.config;


import com.google.common.collect.Lists;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 安全相关的自定义配置属性。
 * 通过 @ConfigurationProperties 注解将 application.yml 中的 app.security 前缀的配置绑定到该类的字段上。
 *
 * @author liusxml
 */
@Data // 使用 Lombok 自动生成 Getter, Setter, toString 等方法
@Configuration
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /**
     * URL 白名单配置 (对应 app.security.permit-all-urls)
     */
    // 初始化以避免 NullPointerException
    private List<String> permitAllUrls = Lists.newArrayList();

}
