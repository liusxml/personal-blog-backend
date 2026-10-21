package com.blog.mcp.tools;

import com.blog.mcp.client.McpSshClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Set;

/**
 * 博客服务与 VPS 服务器只读巡检 MCP 工具集
 *
 * <p><b>核心作用</b>：</p>
 * <ul>
 *   <li>向外部 AI 客户端暴露从"应用健康 ➔ 宿主机资源 ➔ Docker 容器状态 ➔ 容器日志"的四维只读诊断工具。</li>
 * </ul>
 *
 * <p><b>安全设计（Security by Design）</b>：</p>
 * <ol>
 *   <li><b>禁止任意命令执行</b>：不提供传入自由 Shell 命令的接口，所有命令均在代码内部固化为只读指令。</li>
 *   <li><b>严格服务名白名单</b>：{@link #fetchServiceLogs(String)} 方法通过不可变集合 {@link #ALLOWED_SERVICES} 校验参数，
 *       拦截任何非预期的服务名或特殊字符，从根源上杜绝命令注入（Command Injection）风险。</li>
 * </ol>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlogServerOpsTools {

    /**
     * 允许查看日志的 Docker Compose 服务名白名单（对应线上 compose.yml 全部 13 个真实服务）
     */
    private static final Set<String> ALLOWED_SERVICES = Set.of(
            "frontend", "admin", "backend", "redis", "mysql", "caddy", "monitor",
            "grafana", "prometheus", "alloy", "dozzle", "mysql-exporter", "redis-exporter"
    );

    private final RestClient blogRestClient;
    private final McpSshClient mcpSshClient;

    /**
     * 1. 应用层：检查博客后端 Spring Boot Actuator 健康状态
     *
     * @return Actuator /actuator/health 响应 JSON
     */
    @Tool(description = "检查个人博客后端服务 (blog-application) 的 Spring Boot Actuator 健康状态（包括应用存活、MySQL 数据库连接、Redis 缓存等健康指标）。")
    public String checkBackendHealth() {
        log.info("MCP Tool 调用: checkBackendHealth()");
        try {
            return blogRestClient.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("MCP checkBackendHealth 调用失败", e);
            return "博客后端健康检查失败（服务可能未启动或不可达）: " + e.getMessage();
        }
    }

    /**
     * 2. 宿主机层：通过 SSH 检查 VPS 服务器的 CPU 负载、内存占用和磁盘空间
     *
     * @return 服务器资源使用情况汇总文本
     */
    @Tool(description = "通过 SSH 远程检查 VPS 服务器的硬件资源使用情况（包含系统运行时间、CPU 负载 uptime、内存使用率 free -h、根目录磁盘空间 df -h）。")
    public String checkServerResources() {
        log.info("MCP Tool 调用: checkServerResources()");
        // 固化只读命令，一次性采集负载、内存与磁盘信息，减少 SSH 往返次数
        String command = "echo '=== [1] Uptime & Load ===' && uptime "
                + "&& echo '=== [2] Memory Usage ===' && free -h "
                + "&& echo '=== [3] Disk Usage ===' && df -h /";
        return mcpSshClient.executeReadOnlyCommand(command);
    }

    /**
     * 3. 容器编排层：通过 SSH 检查 VPS 上所有 Docker Compose 容器的运行与健康状态
     *
     * @return Docker 容器状态表格文本
     */
    @Tool(description = "通过 SSH 远程检查 VPS 服务器上所有 Docker Compose 服务容器的运行状态与健康检查结果 (docker compose ps)。")
    public String checkDockerContainers() {
        log.info("MCP Tool 调用: checkDockerContainers()");
        String command = String.format(
                "sudo docker compose -f %s/compose.yml ps --format \"table {{.Service}}\\t{{.Status}}\"",
                mcpSshClient.getDeployDir()
        );
        return mcpSshClient.executeReadOnlyCommand(command);
    }

    /**
     * 4. 容器日志层：查看指定 Docker Compose 服务最近 50 行日志
     *
     * @param serviceName 服务名称（必须属于白名单 ALLOWED_SERVICES）
     * @return 容器最近 50 行日志文本
     */
    @Tool(description = "通过 SSH 查看 VPS 上指定 Docker 服务的最近 50 行运行日志。serviceName 参数仅允许传入以下 13 个合法值之一：frontend、admin、backend、redis、mysql、caddy、monitor、grafana、prometheus、alloy、dozzle、mysql-exporter、redis-exporter。")
    public String fetchServiceLogs(
            @ToolParam(description = "服务名称，可选值：frontend / admin / backend / redis / mysql / caddy / monitor / grafana / prometheus / alloy / dozzle / mysql-exporter / redis-exporter") String serviceName) {

        log.info("MCP Tool 调用: fetchServiceLogs(serviceName={})", serviceName);

        if (!StringUtils.hasText(serviceName) || !ALLOWED_SERVICES.contains(serviceName.trim())) {
            return String.format("拒绝执行：非法服务名 [%s]。允许的服务名列表：%s", serviceName, ALLOWED_SERVICES);
        }

        String safeService = serviceName.trim();
        // --no-color 过滤掉终端 ANSI 颜色控制符，避免返回给大模型时夹杂转义乱码
        String command = String.format(
                "sudo docker compose -f %s/compose.yml logs --no-color --tail=50 %s",
                mcpSshClient.getDeployDir(), safeService
        );
        return mcpSshClient.executeReadOnlyCommand(command);
    }
}
