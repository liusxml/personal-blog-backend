package com.blog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson 全局配置
 *
 * <p>
 * 解决 JavaScript Number 精度丢失问题：
 * <ul>
 * <li>JavaScript Number 只有 53 位精度</li>
 * <li>雪花ID（Snowflake ID）是 64 位 long</li>
 * <li>序列化为 JSON 时会丢失精度</li>
 * <li>例如：1999836556117934082 → 1999836556117934000</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>解决方案</strong>：将所有 Long/long 类型序列化为 String
 * </p>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Configuration
public class JacksonConfig {

    /**
     * 配置全局 ObjectMapper
     *
     * <p>
     * 关键配置：
     * <ul>
     * <li>将 Long/long 类型序列化为 String（防止前端精度丢失）</li>
     * <li>适用于所有 REST API 响应中的 long 类型字段</li>
     * <li>特别重要：雪花ID、时间戳等大数字</li>
     * </ul>
     * </p>
     *
     * @param builder Spring Boot 提供的 ObjectMapper 构建器
     * @return 配置好的 ObjectMapper 实例
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        // 创建自定义模块
        SimpleModule simpleModule = new SimpleModule();

        // Long 类型序列化为 String（防止精度丢失）
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);

        // 注册模块
        objectMapper.registerModule(simpleModule);

        return objectMapper;
    }
}
