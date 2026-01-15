package com.blog.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.blog.handler.MyMetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.EnumTypeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus处理器配置类
 * <p>
 * 主要功能：
 * <ul>
 * <li>配置全局属性（如Banner、枚举处理器）</li>
 * <li>注册自定义处理器（如MetaObjectHandler）</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class MybatisPlusHandlerConfig {

    /**
     * 自定义MyBatis-Plus全局配置属性
     * <p>
     * 使用{@link MybatisPlusPropertiesCustomizer}优雅地修改MyBatis-Plus配置，
     * 相比手动创建{@code GlobalConfig}更加灵活且支持Spring Boot属性绑定。
     *
     * <h3>配置项说明</h3>
     * <ul>
     * <li><b>banner</b>: 关闭MyBatis-Plus启动Banner，减少日志噪音</li>
     * <li><b>defaultEnumTypeHandler</b>: 枚举默认处理器</li>
     * </ul>
     *
     * <h3>枚举处理器说明</h3>
     * <p>
     * {@code EnumTypeHandler}将Java枚举映射到数据库：
     * <ul>
     * <li>默认行为：存储枚举的{@code name()}值（字符串形式）</li>
     * <li>数据库示例：{@code Status.ACTIVE} → 存储为 {@code 'ACTIVE'}</li>
     * <li>自定义存储：使用{@code @EnumValue}注解指定存储字段</li>
     * </ul>
     *
     * <pre>
     * // 示例：使用@EnumValue自定义存储值
     * public enum Status {
     *     ACTIVE(1, "激活"),
     *     INACTIVE(0, "禁用");
     *
     *     &#64;EnumValue // 指定存储code值
     *     private final int code;
     *     private final String desc;
     * }
     * // 数据库存储：1或0，而非'ACTIVE'或'INACTIVE'
     * </pre>
     *
     * @return MybatisPlusPropertiesCustomizer实例
     */
    @Bean
    public MybatisPlusPropertiesCustomizer mybatisPlusPropertiesCustomizer() {
        return properties -> {
            // 关闭MyBatis-Plus启动Banner
            properties.getGlobalConfig().setBanner(false);
            log.info("MyBatis-Plus配置: 启动Banner已关闭");

            // 配置默认枚举处理器
            MybatisPlusProperties.CoreConfiguration configuration = properties.getConfiguration();
            if (configuration == null) {
                configuration = new MybatisPlusProperties.CoreConfiguration();
                properties.setConfiguration(configuration);
            }
            configuration.setDefaultEnumTypeHandler(EnumTypeHandler.class);
            log.info("MyBatis-Plus配置: 默认枚举处理器 = EnumTypeHandler（存储name()值）");
        };
    }

    /**
     * 注册MetaObjectHandler自动填充处理器
     * <p>
     * 功能：自动填充审计字段（createTime、updateTime、createBy、updateBy）
     * <p>
     * 详细说明见 {@link MyMetaObjectHandler}
     *
     * @return MyMetaObjectHandler实例
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        log.info("MyBatis-Plus配置: MetaObjectHandler已注册（审计字段自动填充）");
        return new MyMetaObjectHandler();
    }
}
