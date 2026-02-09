package com.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Web相关配置
 *
 * @author liusxml
 * @since 1.0.0
 */
@Configuration
public class WebConfig {

    /**
     * RestTemplate Bean
     *
     * <p>用于HTTP请求，如获取必应壁纸API</p>
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
