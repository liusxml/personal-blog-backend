package com.blog.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 核心配置类
 */
@Configuration
// 扫描 Mapper 接口，告诉 MyBatis-Plus 去哪里找你的 Mapper。
// 请确保这个路径能够覆盖所有业务模块的 mapper 包。
//@MapperScan("com.blog.**.mapper")
// @MapperScan("com.blog")
public class MybatisPlusConfig {

    /**
     * 配置 MyBatis-Plus 插件集合
     *
     * @return MybatisPlusInterceptor
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 1. 添加分页插件
        // 这是新版 MyBatis-Plus（3.4.0+）的推荐配置方式
        // 我们需要指定数据库类型，以便生成正确的分页 SQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 如果未来需要，还可以在这里添加其他插件，例如：
        // 2. 乐观锁插件 (OptimisticLockerInnerInterceptor)
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 3. 防止全表更新与删除插件 (BlockAttackInnerInterceptor)
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }
}
