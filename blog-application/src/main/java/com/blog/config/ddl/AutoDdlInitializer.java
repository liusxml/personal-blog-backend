package com.blog.config.ddl;

import com.baomidou.mybatisplus.autoconfigure.DdlApplicationRunner;
import com.baomidou.mybatisplus.extension.ddl.DdlScriptErrorHandler;
import com.baomidou.mybatisplus.extension.ddl.IDdl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * MyBatis-Plus DDL 初始化器（具备事务能力）。
 * <p>
 * <strong>核心职责</strong>：在应用启动时自动执行数据库DDL脚本，确保数据库表结构与实体类定义一致。
 * </p>
 *
 * <h3>设计特性</h3>
 * <ul>
 * <li><strong>事务管理</strong>：整个DDL执行过程在一个事务中完成，失败时自动回滚</li>
 * <li><strong>环境隔离</strong>：通过 {@code @Profile("!test")} 排除测试环境</li>
 * <li><strong>配置驱动</strong>：必须显式配置 {@code mybatis-plus.auto-ddl.enabled=true}
 * 才启用</li>
 * <li><strong>优先执行</strong>：{@code @Order(0)} 确保在其他 ApplicationRunner 之前执行</li>
 * </ul>
 *
 * <h3>使用场景</h3>
 * <ul>
 * <li>✅ 开发环境：自动同步数据库表结构，提升开发效率</li>
 * <li>✅ 测试环境：可选启用，用于集成测试前的数据库初始化</li>
 * <li>❌ 生产环境：<strong>不推荐</strong>，应使用专业数据库迁移工具（如Flyway）</li>
 * </ul>
 *
 * <h3>配置示例</h3>
 *
 * <pre>{@code
 * # application-dev.yaml
 * mybatis-plus:
 *   auto-ddl:
 *     enabled: true  # 开发环境启用
 * }</pre>
 *
 * @author liusxml
 * @see com.baomidou.mybatisplus.autoconfigure.DdlApplicationRunner
 * @see org.springframework.boot.ApplicationRunner
 * @since 1.0.0
 */
@Slf4j
@Configuration
@Order(0) // 可选：设置高优先级，确保它在其他 ApplicationRunner 之前执行
@Profile("!test") // 关键新增：表示当 "test" profile 未激活时，此Bean才生效
@ConditionalOnProperty(name = "mybatis-plus.auto-ddl.enabled", havingValue = "true", matchIfMissing = false // 默认禁用，必须显式配置才启用
)
public class AutoDdlInitializer implements ApplicationRunner {

    /**
     * Spring 自动注入的所有 {@link IDdl} 实现类。
     * <p>
     * 默认包含：{@link DdlScriptManager}
     * </p>
     */
    private final List<IDdl> ddlList;

    public AutoDdlInitializer(List<IDdl> ddlList) {
        this.ddlList = ddlList;
    }

    /**
     * Spring Boot 应用启动时的入口方法。
     * <p>
     * <strong>执行流程</strong>：
     * <ol>
     * <li>检查是否存在 {@link IDdl} 实现类</li>
     * <li>创建 {@link DdlApplicationRunner} 实例（非Spring Bean）</li>
     * <li>在事务中执行所有DDL脚本</li>
     * <li>提交事务或回滚（出错时）</li>
     * </ol>
     * </p>
     *
     * <p>
     * <strong>事务保证</strong>：{@code @Transactional} 注解确保整个方法体在一个数据库事务中执行。
     * 方法成功返回后，Spring 事务管理器将自动提交事务；若抛出异常，自动回滚。
     * </p>
     *
     * @param args 应用启动参数
     * @throws Exception DDL 执行失败时抛出异常
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (this.ddlList == null || this.ddlList.isEmpty()) {
            log.warn("No IDdl implementations found by Spring. DDL auto-execution will be skipped.");
            return;
        }

        log.info("📋 Found {} IDdl implementation(s). Starting transactional DDL execution...", ddlList.size());
        this.ddlList.forEach(ddl -> log.info("  ├─ Using IDdl bean: {}", ddl.getClass().getName()));

        // 在方法内部创建 DdlApplicationRunner 实例（局部变量，非Spring Bean）
        // 这样可以避免 Spring 自动运行它，确保只执行一次
        DdlApplicationRunner runner = getDdlApplicationRunner();

        // 执行 DDL 逻辑
        runner.run(args);

        log.info("✅ Transactional DDL execution finished successfully. Spring will now commit the transaction.");
    }

    /**
     * 工厂方法：创建配置完整的 {@link DdlApplicationRunner} 实例。
     * <p>
     * <strong>配置项</strong>：
     * <ul>
     * <li>{@code autoCommit = false} - 由外层事务管理提交</li>
     * <li>{@code throwException = true} - 遇到错误立即中断</li>
     * <li>{@code stopOnError = true} - 脚本执行失败时停止</li>
     * </ul>
     * </p>
     *
     * @return 完全配置好的 DdlApplicationRunner 实例
     */
    private DdlApplicationRunner getDdlApplicationRunner() {
        DdlApplicationRunner runner = new DdlApplicationRunner(ddlList);

        // 事务管理：由外层 @Transactional 控制提交
        runner.setAutoCommit(false);

        // 错误处理：遇到错误立即抛出异常
        runner.setDdlScriptErrorHandler(DdlScriptErrorHandler.ThrowsErrorHandler.INSTANCE);
        runner.setThrowException(true);

        // 配置底层 ScriptRunner 执行策略
        runner.setScriptRunnerConsumer(scriptRunner -> {
            log.debug("Applying advanced customizations to the underlying ScriptRunner...");
            scriptRunner.setLogWriter(null); // 使用 SLF4J 而非标准输出
            scriptRunner.setErrorLogWriter(null); // 错误日志也使用 SLF4J
            scriptRunner.setStopOnError(true); // 遇到错误立即停止
            scriptRunner.setRemoveCRs(false); // 保留回车符
            log.debug("ScriptRunner customization complete.");
        });
        return runner;
    }
}
