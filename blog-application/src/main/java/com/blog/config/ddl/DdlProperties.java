package com.blog.config.ddl;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus Auto DDL Configuration Properties.
 *
 * <p>
 * Binds configuration from application.yaml with prefix
 * "mybatis-plus.auto-ddl".
 * </p>
 *
 * <pre>
 * Example configuration:
 * mybatis-plus:
 *   auto-ddl:
 *     enabled: true
 *     script-dir: db/
 * </pre>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mybatis-plus.auto-ddl")
public class DdlProperties {

    /**
     * Whether to enable Auto DDL feature.
     * Default: false (must be explicitly enabled)
     */
    private boolean enabled = false;

    /**
     * DDL script directory path (relative to classpath).
     * Default: db/
     * Example: db/, sql/, migration/
     */
    private String scriptDir = "db/";

    /**
     * Get the script scanning pattern for Spring ResourcePatternResolver.
     *
     * @return Scan pattern, e.g.: "classpath*:db/**&#47;*.sql"
     */
    public String getScriptPattern() {
        // Ensure path ends with /
        String dir = scriptDir.endsWith("/") ? scriptDir : scriptDir + "/";
        return "classpath*:" + dir + "**/*.sql";
    }
}
