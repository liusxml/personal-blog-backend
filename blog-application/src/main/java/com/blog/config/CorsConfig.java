package com.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * CORS 跨域配置
 *
 * <p>
 * 允许前端从不同域名访问后端API。
 * 开发环境允许所有来源，生产环境应该配置具体的域名。
 * </p>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许的源（开发环境允许所有，生产环境应指定具体域名）
        config.setAllowedOriginPatterns(Arrays.asList("*"));

        // 允许携带凭证（cookies）
        config.setAllowCredentials(true);

        // 允许的HTTP方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 允许的请求头
        config.setAllowedHeaders(Arrays.asList("*"));

        // 暴露的响应头
        config.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Total-Count"));

        // 预检请求的有效期（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
