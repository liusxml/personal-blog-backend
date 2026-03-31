package com.blog.ai.application.tools;

import com.blog.ai.config.OpsProperties;
import com.blog.ai.infrastructure.ssh.OpsCommandWhitelist;
import com.blog.ai.infrastructure.ssh.RemoteSshExecutor;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.blog.ai.infrastructure.ssh.SseEmitterContext;

/**
 * Ops Agent 运维工具集（LangChain4j Function Calling 工具）
 *
 * <p>
 * 此类中的每个 {@code @Tool} 方法都是一个 AI 可调用的"函数"。
 * 大模型在对话中识别到运维意图后，会自动选择并调用对应方法。
 * </p>
 *
 * <p><b>安全保障（双保险）</b>：</p>
 * <ol>
 *   <li>每个方法的 JavaDoc 明确声明合法 serviceName，约束大模型输出范围。</li>
 *   <li>{@link OpsCommandWhitelist} 在运行时进行程序级二次校验，拦截任何违规调用。</li>
 * </ol>
 *
 * @author liusxml
 * @since 1.3.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteDevOpsTools {

    private final RemoteSshExecutor sshExecutor;

    private final OpsProperties opsProperties;

    // ── 工具：拉取最新镜像并重启服务 ─────────────────────────────────────────

    /**
     * 拉取指定前端/管理后台服务的最新 Docker 镜像，并原地重启。
     *
     * <p><b>合法的 serviceName 值（严格限定）</b>：</p>
     * <ul>
     *   <li>{@code frontend} — 博客前台</li>
     *   <li>{@code admin}    — 管理后台</li>
     * </ul>
     *
     * <p><b>注意</b>：backend 禁止通过此方法更新，请使用 {@link #updateBackendSelf}。</p>
     *
     * @param serviceName 待更新的服务名（仅限 frontend / admin）
     * @param sessionId   会话 ID（从记忆上下文自动注入）
     * @return SSH 执行结果（标准输出与错误汇总）
     */
    @Tool("拉取指定服务（frontend 或 admin）的最新 Docker 镜像并重启容器。serviceName 只能是 frontend 或 admin，backend 禁止使用此工具。")
    public String updateService(String serviceName, @ToolMemoryId String sessionId) {
        OpsCommandWhitelist.validate(serviceName, OpsCommandWhitelist.UPDATABLE_SERVICES);
        String deployDir = opsProperties.getDeployDir();
        String command = String.format(
                "sudo docker compose -f %s/compose.yml pull %s && sudo docker compose -f %s/compose.yml up -d %s",
                deployDir, serviceName, deployDir, serviceName
        );
        log.info("Ops updateService: service=[{}]", serviceName);
        SseEmitter emitter = SseEmitterContext.get(sessionId);
        return sshExecutor.execute(command, emitter);
    }

    // ── 工具：原地重启服务（不拉取新镜像） ────────────────────────────────────

    /**
     * 原地重启指定服务容器，不拉取新镜像。
     *
     * <p><b>合法的 serviceName 值</b>：{@code frontend} / {@code admin} / {@code cache} / {@code db}</p>
     *
     * <p><b>适用场景</b>：服务无响应、OOM 崩溃后需要快速恢复，无需更新镜像版本。</p>
     *
     * @param serviceName 待重启的服务名（frontend / admin / cache / db）
     * @param sessionId   会话 ID
     * @return SSH 执行结果
     */
    @Tool("原地重启指定服务（不更新镜像）。serviceName 可选值：frontend / admin / redis / mysql / caddy。backend 不支持，请使用 updateBackendSelf。")
    public String restartService(String serviceName, @ToolMemoryId String sessionId) {
        OpsCommandWhitelist.validate(serviceName, OpsCommandWhitelist.RESTARTABLE_SERVICES);
        String command = String.format(
                "sudo docker compose -f %s/compose.yml restart %s",
                opsProperties.getDeployDir(), serviceName
        );
        log.info("Ops restartService: service=[{}]", serviceName);
        SseEmitter emitter = SseEmitterContext.get(sessionId);
        return sshExecutor.execute(command, emitter);
    }

    // ── 工具：查看服务日志 ────────────────────────────────────────────────────

    /**
     * 查看指定服务最近 100 行的 Docker 日志（只读操作）。
     *
     * <p><b>合法的 serviceName 值</b>：{@code frontend} / {@code admin} / {@code backend} / {@code cache} / {@code db}</p>
     *
     * @param serviceName 待查看日志的服务名（支持全部服务）
     * @param sessionId   会话 ID
     * @return 返回抽取的日志内容（作为回答喂给大模型）
     */
    @Tool("查看指定服务最近 100 行日志。serviceName 可选值：frontend / admin / backend / redis / mysql / caddy / monitor。")
    public String fetchLogs(String serviceName, @ToolMemoryId String sessionId) {
        OpsCommandWhitelist.validate(serviceName, OpsCommandWhitelist.LOGGABLE_SERVICES);
        String command = String.format(
                // --no-color：屏蔽 ANSI 颜色控制序列，避免前端终端出现乱码
                "sudo docker compose -f %s/compose.yml logs --no-color --tail=100 %s",
                opsProperties.getDeployDir(), serviceName
        );
        log.info("Ops fetchLogs: service=[{}]", serviceName);
        SseEmitter emitter = SseEmitterContext.get(sessionId);
        return sshExecutor.execute(command, emitter);
    }

    // ── 工具：查看全部服务运行状态 ───────────────────────────────────────────

    /**
     * 检查所有 Docker Compose 服务的运行状态。
     *
     * <p>
     * 执行 {@code docker compose ps}，返回所有服务的健康状态、启动时间等。
     * 无需传入 serviceName，一次性返回全部服务，适合用户询问"服务状态怎么样"。
     * </p>
     *
     * @param sessionId 会话 ID
     * @return 所有服务的状态表格
     */
    @Tool("检查所有 Docker Compose 服务的运行状态（健康状态、启动时间等）。无需传入 serviceName，一次性返回全部服务状态。用户询问服务器/服务状态时优先使用此工具，而不是 fetchLogs。")
    public String checkServiceStatus(@ToolMemoryId String sessionId) {
        String command = String.format(
                // --format 只保留服务名和状态，Status 里已包含健康信息（如 "Up 7 days (healthy)"）
                // 去掉 COMMAND（启动命令太长会乱码）、IMAGE、PORTS（含大量无关信息）
                "sudo docker compose -f %s/compose.yml ps --format \"table {{.Service}}\\t{{.Status}}\"",
                opsProperties.getDeployDir()
        );
        log.info("Ops checkServiceStatus: 检查所有服务状态");
        SseEmitter emitter = SseEmitterContext.get(sessionId);
        return sshExecutor.execute(command, emitter);
    }

    // ── 工具：更新 backend 自身（特殊路径） ───────────────────────────────────

    /**
     * 更新并重启 backend 服务自身。
     *
     * <p><b>为什么与其他服务不同？</b><br>
     * 若 backend 以普通方式执行 {@code docker compose up -d backend}，
     * 该命令会杀死当前 JVM 进程，导致 SSH 频道中断、命令永远挂起。
     * 因此必须通过 {@code nohup} 将执行延迟到新进程中，让当前 SSH 会话能正常退出。
     * </p>
     *
     * <p><b>此方法无 serviceName 参数</b>，固定操作 backend，无需白名单校验。</p>
     *
     * @param sessionId   会话 ID
     * @return 执行状态结果
     */
    @Tool("更新 backend 服务自身（拉取最新镜像并重启）。此操作使用 nohup 延迟执行，防止自杀悖论。无需传入 serviceName。")
    public String updateBackendSelf(@ToolMemoryId String sessionId) {
        String deployDir = opsProperties.getDeployDir();
        // nohup 延迟 3 秒后执行，确保当前 SSH 会话能先正常返回
        String command = String.format(
                "nohup sh -c 'sleep 3 && sudo docker compose -f %s/compose.yml pull backend && sudo docker compose -f %s/compose.yml up -d backend' > /tmp/backend-update.log 2>&1 &",
                deployDir, deployDir
        );
        log.info("Ops updateBackendSelf: nohup 异步触发 backend 更新");
        SseEmitter emitter = SseEmitterContext.get(sessionId);
        sshExecutor.execute(command, emitter);
        return "Backend 重启指令已发射。预计将在 3 秒内断开连接，后台执行重启操作。";
    }
}
