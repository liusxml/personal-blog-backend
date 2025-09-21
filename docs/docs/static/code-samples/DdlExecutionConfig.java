package com.blog.config.ddl;

import com.baomidou.mybatisplus.autoconfigure.DdlApplicationRunner;
import com.baomidou.mybatisplus.extension.ddl.DdlScriptErrorHandler;
import com.baomidou.mybatisplus.extension.ddl.IDdl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * DDL 执行器配置。
 * <p>
 * 通过自定义 DdlApplicationRunner Bean 来精细化控制 DDL 脚本的执行行为。
 * 例如开启事务、设置错误处理策略等，这对于生产环境至关重要。
 *
 * @author YourName
 */
@Slf4j // 使用 @Slf4j 注解自动注入 log 实例
@Configuration
public class DdlExecutionConfig {

    /**
     * 注入并配置 DDL 运行器。
     *
     * @param ddlList Spring 容器中所有的 IDdl 实现实例，如果未找到则可能为空列表。
     * @return 配置好的 DdlApplicationRunner 实例。
     */
    @Bean
    public DdlApplicationRunner ddlApplicationRunner(List<IDdl> ddlList) {
        // 1. 前置条件检查和日志记录
        if (ddlList == null || ddlList.isEmpty()) {
            log.warn("No IDdl implementations found by Spring. DDL auto-execution will be skipped.");
            // 返回一个无操作的 runner 实例，避免空指针，并保持应用正常启动
            return new DdlApplicationRunner(List.of());
        }

        log.info("Found {} IDdl implementation(s). Configuring DdlApplicationRunner...", ddlList.size());
        ddlList.forEach(ddl -> log.info(" -> Discovered IDdl bean: {}", ddl.getClass().getName()));

        // 2. 创建并配置 Runner 实例
        DdlApplicationRunner runner = new DdlApplicationRunner(ddlList);

        // 3. 核心执行策略配置 (关键生产配置)
        //    - 事务控制: 每个脚本文件在一个独立的事务中执行，失败则回滚。
        runner.setAutoCommit(false);
        log.debug("DDL runner configured with setAutoCommit(false).");

        //    - 错误处理策略: 遇到 SQL 错误时，立即抛出异常。
        runner.setDdlScriptErrorHandler(DdlScriptErrorHandler.ThrowsErrorHandler.INSTANCE);
        log.debug("DDL runner configured with ThrowsErrorHANDLER error handler.");

        //    - 中断应用启动: 如果 DDL 执行抛出任何异常，则中断 Spring Boot 的启动过程。
        runner.setThrowException(true);
        log.debug("DDL runner configured with setThrowEx(true).");

        // 4. 底层 ScriptRunner 高级定制
        //    使用 Consumer Lambda 对 Mybatis 原生的 ScriptRunner 进行深度配置
        runner.setScriptRunnerConsumer(scriptRunner -> {
            log.debug("Applying advanced customizations to the underlying ScriptRunner...");
            //    - 日志重定向: 关闭 ScriptRunner 内置的 System.out/System.err 日志。
            //      让所有日志输出都由 SLF4J/Logback 统一管理，避免了控制台杂乱的输出。
            scriptRunner.setLogWriter(null);
            scriptRunner.setErrorLogWriter(null);

            //    - 错误即停 (双重保险): 再次确认遇到错误时立即停止执行。
            //      虽然上层已有 ThrowsErrorHANDLER，但这是 Mybatis ScriptRunner 自身的保险开关。
            scriptRunner.setStopOnError(true);
            //    - 保持换行符: 禁用对\r\n的转换，确保脚本在不同操作系统（Windows/Linux）下行为一致。
            scriptRunner.setRemoveCRs(false);
            log.debug("ScriptRunner customization complete.");
        });

        log.info("DdlApplicationRunner has been successfully configured and is ready to execute.");
        return runner;
    }
}
