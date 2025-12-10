package com.blog.config.ddl;

import com.baomidou.mybatisplus.extension.ddl.IDdl;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
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

@Slf4j
@Component
@Profile("!test")
@ConditionalOnProperty(name = "mybatis-plus.auto-ddl.enabled", havingValue = "true", matchIfMissing = false)
public class DdlScriptManager implements IDdl {

    private final DataSource dataSource;
    private final ApplicationContext applicationContext;
    private final DdlProperties ddlProperties;

    public DdlScriptManager(
            DataSource dataSource,
            ApplicationContext applicationContext,
            DdlProperties ddlProperties) {
        this.dataSource = dataSource;
        this.applicationContext = applicationContext;
        this.ddlProperties = ddlProperties;
    }

    @Override
    public void runScript(Consumer<DataSource> consumer) {
        consumer.accept(dataSource);
    }

    @Override
    public List<String> getSqlFiles() {

        try {
            String pattern = ddlProperties.getScriptPattern();
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
                    applicationContext.getClassLoader());

            log.info("🔍 Scanning for DDL scripts with pattern: {}", pattern);
            Resource[] resources = resolver.getResources(pattern);

            if (resources.length == 0) {
                log.warn("⚠️  No DDL scripts found in directory '{}'. Database schema may be incomplete!",
                        ddlProperties.getScriptDir());
                log.warn("⚠️  Please ensure SQL scripts are placed in the correct directory.");
                return Collections.emptyList();
            }

            List<String> scriptPaths = Arrays.stream(resources)
                    .map(resource -> {
                        try {
                            String fullPath = resource.getURL().getPath();
                            int startIndex = fullPath.lastIndexOf(ddlProperties.getScriptDir());
                            if (startIndex != -1) {
                                return fullPath.substring(startIndex);
                            }
                            return ddlProperties.getScriptDir() + "/" + resource.getFilename();
                        } catch (IOException e) {
                            log.error("Failed to resolve path for DDL resource: {}", resource.getFilename(), e);
                            return null;
                        }
                    })
                    .filter(StringUtils::isNotBlank)
                    .sorted()
                    .collect(Collectors.toList());

            log.info("📋 DDL Execution Plan ({} script{}):", scriptPaths.size(), scriptPaths.size() > 1 ? "s" : "");
            scriptPaths.forEach(path -> log.info("  ├─ {}", path));

            validateScriptNaming(scriptPaths);

            return scriptPaths;

        } catch (IOException e) {
            log.error("❌ FATAL: Failed to scan for DDL scripts in directory '{}'", ddlProperties.getScriptDir(), e);

            if (isProductionLike()) {
                throw new IllegalStateException("DDL script scanning failed in production environment", e);
            }

            log.warn("⚠️  Continuing startup without DDL execution (non-production environment)");
            return Collections.emptyList();
        }
    }

    private boolean isProductionLike() {
        String[] activeProfiles = applicationContext.getEnvironment().getActiveProfiles();
        return Arrays.asList(activeProfiles).contains("prod") || Arrays.asList(activeProfiles).contains("production");
    }

    private void validateScriptNaming(List<String> scriptPaths) {
        scriptPaths.forEach(script -> {

            String filename = script.substring(script.lastIndexOf('/') + 1);
            if (!filename.matches("V\\d+\\.\\d+\\.\\d+__.*\\.sql")) {
                log.warn("⚠️  Script '{}' does not follow recommended naming convention 'Vx.y.z__description.sql'",
                        filename);
                log.warn("   Recommended example: V1.0.0__init_schema.sql");
            }
        });
    }
}
