package com.blog.ai.infrastructure.ssh;

import com.blog.ai.api.enums.OpsErrorCode;
import com.blog.common.exception.BusinessException;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * Ops Agent 命令安全白名单
 *
 * <p>
 * 程序级硬防线：所有 {@code @Tool} 方法在执行 SSH 命令前，必须经过此类校验。
 * 这一层守卫确保大模型的 Function Calling 输出无论何时都不能突破边界，
 * 即使遭遇 Prompt 注入攻击也无法让 AI 操作未授权的服务或执行危险命令。
 * </p>
 *
 * <p>服务分组说明：</p>
 * <ul>
 *   <li>{@link #UPDATABLE_SERVICES}：可拉取新镜像并重启的服务（不含 backend，防自杀悖论）</li>
 *   <li>{@link #RESTARTABLE_SERVICES}：可原地重启的服务（不重新拉取镜像）</li>
 *   <li>{@link #LOGGABLE_SERVICES}：可查看日志的服务（包含 backend，仅读操作安全）</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.3.0
 */
@Slf4j
public final class OpsCommandWhitelist {

    private OpsCommandWhitelist() {
        // 工具类，禁止实例化
    }

    /**
     * 允许执行 docker compose pull + up -d 的服务名集合。
     * <b>不包含 backend</b>：backend 自更新会杀死执行线程，需走专属的 nohup 延迟路径。
     */
    public static final Set<String> UPDATABLE_SERVICES = ImmutableSet.of(
            "frontend",
            "admin"
    );

    /**
     * 允许执行 docker compose restart 的服务名集合（原地重启，不拉取新镜像）。
     */
    public static final Set<String> RESTARTABLE_SERVICES = ImmutableSet.of(
            "frontend",
            "admin",
            "redis",    // Redis 缓存（原名 cache，已更正）
            "mysql",    // MySQL 数据库（原名 db，已更正）
            "caddy"     // 反向代理（Caddy）
    );

    /**
     * 允许查看日志的服务名集合（只读，包含 backend）。
     */
    public static final Set<String> LOGGABLE_SERVICES = ImmutableSet.of(
            "frontend",
            "admin",
            "backend",
            "redis",    // Redis 缓存（原名 cache，已更正）
            "mysql",    // MySQL 数据库（原名 db，已更正）
            "caddy",    // 反向代理
            "monitor"   // Spring Boot Admin 监控
    );

    /**
     * 校验服务名是否在指定白名单中，不通过则记录警告日志并抛出 {@link BusinessException}。
     *
     * <p>使用 {@link StringUtils#isBlank} 进行空值判断（规范：不使用手动 == null）。</p>
     *
     * @param serviceName 大模型传入的服务名参数
     * @param allowedSet  本次操作对应的白名单集合（从上方常量中选取）
     * @throws BusinessException 错误码 {@link OpsErrorCode#FORBIDDEN_SERVICE}
     */
    public static void validate(String serviceName, Set<String> allowedSet) {
        if (StringUtils.isBlank(serviceName) || !allowedSet.contains(serviceName.toLowerCase())) {
            log.warn("Ops 白名单拦截：非法服务名 [{}]，合法值为 {}", serviceName, allowedSet);
            throw new BusinessException(OpsErrorCode.FORBIDDEN_SERVICE);
        }
    }
}
