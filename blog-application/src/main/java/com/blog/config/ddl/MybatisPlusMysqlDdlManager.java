package com.blog.config.ddl;

import com.baomidou.mybatisplus.extension.ddl.IDdl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Mybatis-Plus DDL 脚本管理器。
 * <p>
 * 实现了从 classpath 指定目录下动态加载所有 .sql 文件。
 * 保证了 DDL 脚本的维护与代码分离，更易于管理。
 */
@Slf4j
@Component
public class MybatisPlusMysqlDdlManager implements IDdl {

    @Autowired
    private DataSource dataSource;

    // 从 application.yml 读取 DDL 脚本目录，并提供一个安全的默认值
    @Value("${mybatis-plus.ddl.dir.mysql:db/}")
    private String ddlDirPath;

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void runScript(Consumer<DataSource> consumer) {
        consumer.accept(dataSource);
    }

    /**
     * 动态扫描并返回需要执行的 SQL 脚本文件列表。
     * <p>
     * 使用 Spring 的 ResourcePatternResolver 来扫描 classpath，
     * 确保在 IDE 和 JAR 包环境中都能正确工作。
     * <b>注意:</b> 脚本将按文件名自然排序执行，请使用版本号前缀命名 (e.g., v1.0.0__, v1.0.1__ )。
     *
     * @return DDL 脚本文件路径列表
     */
    @Override
    public List<String> getSqlFiles() {
        try {
            // 构建 classpath 扫描路径, e.g., "classpath*:db/mysql/*.sql"
            String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + ddlDirPath + "/**/*.sql";
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(applicationContext.getClassLoader());

            log.info("Scanning for DDL scripts with pattern: {}", pattern);
            Resource[] resources = resolver.getResources(pattern);

            if (resources.length == 0) {
                log.warn("No DDL scripts found in directory '{}'.", ddlDirPath);
                return Collections.emptyList();
            }

            List<String> scriptPaths = Arrays.stream(resources)
                    .map(resource -> {
                        try {
                            // 我们需要返回相对于 classpath 的路径，例如 "db/mysql/v1.0.0_init.sql"
                            String fullPath = resource.getURL().getPath();
                            // 查找 ddlDirPath 在完整路径中的位置，并截取之后的部分
                            int startIndex = fullPath.lastIndexOf(ddlDirPath);
                            if (startIndex != -1) {
                                return fullPath.substring(startIndex);
                            }
                            // 如果无法定位，提供一个备用方案（可能不适用于所有情况）
                            return ddlDirPath + "/" + resource.getFilename();
                        } catch (IOException e) {
                            log.error("Failed to resolve path for DDL resource: {}", resource.getFilename(), e);
                            return null;
                        }
                    })
                    .filter(StringUtils::isNotBlank)
                    .sorted() // 按文件名自然排序，确保执行顺序
                    .collect(Collectors.toList());

            log.info("Found DDL scripts to be executed: {}", scriptPaths);
            return scriptPaths;

        } catch (IOException e) {
            log.error("Failed to scan for DDL scripts in directory '{}': {}", ddlDirPath, e.getMessage(), e);
            // 在扫描失败时，应抛出异常或返回空列表，具体取决于业务需求。
            // 返回空列表意味着应用会继续启动，但数据库结构可能不正确。
            return Collections.emptyList();
        }
    }
}
