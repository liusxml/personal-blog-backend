package com.blog.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus核心配置类
 * <p>
 * 配置MyBatis-Plus插件拦截器，提供以下功能：
 * <ul>
 * <li>防全表更新删除 - 防止误操作</li>
 * <li>分页查询 - 物理分页</li>
 * <li>乐观锁 - 并发控制</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class MybatisPlusConfig {

    /**
     * 当前运行环境
     */
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 配置MyBatis-Plus插件拦截器
     * <p>
     * 插件执行顺序：
     * <ol>
     * <li>BlockAttackInnerInterceptor - 防全表操作</li>
     * <li>PaginationInnerInterceptor - 分页查询</li>
     * <li>OptimisticLockerInnerInterceptor - 乐观锁</li>
     * </ol>
     * 
     * <p>
     * 注意：插件顺序会影响执行逻辑，请勿随意调整
     *
     * @return MybatisPlusInterceptor实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // =======================================================================
        // 1. 防全表更新删除插件
        // =======================================================================
        // 功能：阻止恶意的全表操作
        // - UPDATE不带WHERE条件 → 抛出异常
        // - DELETE不带WHERE条件 → 抛出异常
        // 适用场景：防止开发失误导致数据丢失
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        // =======================================================================
        // 2. 分页插件
        // =======================================================================
        // 功能：自动完成分页查询
        // - 自动COUNT查询
        // - 自动生成LIMIT语句
        // - 支持多数据库方言
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(500L); // 单页最大数量，防止恶意查询
        paginationInterceptor.setOverflow(false); // 溢出总页数后是否处理(false=返回空)
        paginationInterceptor.setOptimizeJoin(true); // 优化JOIN的COUNT SQL
        interceptor.addInnerInterceptor(paginationInterceptor);

        // =======================================================================
        // 3. 乐观锁插件
        // =======================================================================
        // 功能：基于版本号的并发控制
        // - 更新时自动检查version字段
        // - 更新成功后version自动+1
        // 使用：Entity字段添加@Version注解
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 开发/测试环境输出插件配置信息
        if ("dev".equals(activeProfile) || "test".equals(activeProfile)) {
            log.info("════════════════════════════════════════════════════════");
            log.info("MyBatis-Plus插件配置（环境: {}）", activeProfile);
            log.info("  ✓ 防全表更新删除插件 - 已启用");
            log.info("  ✓ 分页插件 - 已启用（最大单页:500条，优化JOIN:true）");
            log.info("  ✓ 乐观锁插件 - 已启用");
            log.info("════════════════════════════════════════════════════════");
        }

        return interceptor;
    }
}
