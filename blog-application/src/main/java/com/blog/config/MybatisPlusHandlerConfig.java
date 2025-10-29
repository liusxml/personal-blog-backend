package com.blog.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.blog.handler.MyMetaObjectHandler;
import org.apache.ibatis.type.EnumTypeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusHandlerConfig {

    /**
     * 使用 MybatisPlusPropertiesCustomizer 自定义 MybatisPlus 的配置属性
     * 例如：关闭 Banner、配置默认枚举处理器等
     */
    @Bean
    public MybatisPlusPropertiesCustomizer mybatisPlusPropertiesCustomizer() {
        return properties -> {
            // 设置全局配置
            properties.getGlobalConfig().setBanner(false);
            // 正确方式：获取或创建 configuration 对象，然后设置其属性
            MybatisPlusProperties.CoreConfiguration configuration = properties.getConfiguration();
            if (configuration == null) {
                configuration = new MybatisPlusProperties.CoreConfiguration();
                properties.setConfiguration(configuration);
            }
            // 配置默认的枚举处理器为 MyBatis 内置的 EnumTypeHandler
            configuration.setDefaultEnumTypeHandler(EnumTypeHandler.class);
        };
    }

    /**
     * 注册 MetaObjectHandler 自动填充处理器
     *
     * @return MyMetaObjectHandler 的 Bean 实例
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MyMetaObjectHandler();
    }

}
