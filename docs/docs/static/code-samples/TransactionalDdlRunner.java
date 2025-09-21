package com.blog.config.ddl;

import com.baomidou.mybatisplus.autoconfigure.DdlApplicationRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 一个带有事务的 DDL 执行器。
 * <p>
 * 通过实现 ApplicationRunner 并使用 @Transactional 注解，
 * 确保 MyBatis-Plus DDL 的所有操作（包括插入历史记录）都在同一个事务中完成。
 * 当方法成功结束时，Spring 会自动提交事务。
 *
 * @author YourName
 */
@Slf4j
@Component
public class TransactionalDdlRunner implements ApplicationRunner {

    // 注入我们之前已经配置好的 DdlApplicationRunner
    @Autowired
    private DdlApplicationRunner ddlApplicationRunner;

    /**
     * 此方法由 Spring Boot 在应用启动时自动调用。
     * @Transactional 注解会为整个方法的执行开启一个事务。
     */
    @Override
    @Transactional // 这是解决问题的关键！
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting DDL execution within a managed transaction...");
        
        // 调用 MyBatis-Plus DdlApplicationRunner 的 run 方法，
        // 它的所有数据库操作都会在这个 @Transactional 的事务中进行。
        ddlApplicationRunner.run(args);

        log.info("DDL execution completed successfully. Transaction will be committed.");
        // 方法正常退出后，Spring 会自动提交事务，ddl_history 的插入操作将被持久化。
        // 如果ddlApplicationRunner.run()抛出异常，Spring会自动回滚事务。
    }
}
