package com.blog.config.ddl;

import com.baomidou.mybatisplus.autoconfigure.DdlApplicationRunner;
import com.baomidou.mybatisplus.extension.ddl.DdlScriptErrorHandler;
import com.baomidou.mybatisplus.extension.ddl.IDdl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 具备事务能力的 DDL 初始化器。
 * <p>
 * 该类作为唯一的 ApplicationRunner 来处理 DDL 脚本，解决了 DDL 逻辑被重复执行的问题。
 * 整个 run 方法被 @Transactional 注解包裹，确保所有数据库操作（包括插入历史记录）
 * 都在一个原子事务中完成。
 * </p>
 */
@Slf4j
@Configuration
@Order(0) // 可选：设置高优先级，确保它在其他 ApplicationRunner 之前执行
@Profile("!test") // 关键新增：表示当 "test" profile 未激活时，此Bean才生效
public class DdlInitializer implements ApplicationRunner {

    // Spring 会自动注入所有 IDdl 接口的实现，也就是我们的 MybatisPlusMysqlDdlManager
    @Autowired
    private List<IDdl> ddlList;

    /**
     * Spring Boot 启动时将调用此方法。
     * @Transactional 注解保证了整个方法体在一个数据库事务中执行。
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (this.ddlList == null || this.ddlList.isEmpty()) {
            log.warn("No IDdl implementations found by Spring. DDL auto-execution will be skipped.");
            return;
        }

        log.info("Found {} IDdl implementation(s). Starting transactional DDL execution...", ddlList.size());
        this.ddlList.forEach(ddl -> log.info(" -> Using IDdl bean: {}", ddl.getClass().getName()));

        // 关键：在方法内部创建 DdlApplicationRunner 实例。
        // 它是一个局部变量，而不是一个被 Spring 管理的 Bean，所以 Spring 不会再次自动运行它。
        DdlApplicationRunner runner = new DdlApplicationRunner(ddlList);

        // 应用所有必要的配置
        runner.setAutoCommit(false);
        runner.setDdlScriptErrorHandler(DdlScriptErrorHandler.ThrowsErrorHandler.INSTANCE);
        runner.setThrowException(true);

        // 配置底层的 ScriptRunner
        runner.setScriptRunnerConsumer(scriptRunner -> {
            log.debug("Applying advanced customizations to the underlying ScriptRunner...");
            scriptRunner.setLogWriter(null);       // 使用 SLF4J
            scriptRunner.setErrorLogWriter(null);  // 使用 SLF4J
            scriptRunner.setStopOnError(true);
            scriptRunner.setRemoveCRs(false);
            log.debug("ScriptRunner customization complete.");
        });

        // 执行 DDL 逻辑
        runner.run(args);

        log.info("Transactional DDL execution finished. Spring will now commit the transaction.");
        // 方法成功返回后，Spring 的事务管理器将自动提交事务。
    }
}
