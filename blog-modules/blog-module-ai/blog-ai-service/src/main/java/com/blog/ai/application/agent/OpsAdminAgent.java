package com.blog.ai.application.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.memory.ChatMemoryAccess;

/**
 * Ops 运维 AI Agent 接口（LangChain4j AiServices 代理）
 *
 * <p>
 * 此接口由 LangChain4j 在运行时动态代理实现（{@code AiServices.builder(...).build()}）。
 * 调用者无需实现此接口，只需注入并调用 {@link #chat} 方法。
 * </p>
 *
 * <p>
 * 返回 {@link TokenStream} 而非 {@code String}，使 Agent 的推理过程和工具调用结果
 * 可以以流式方式推送到前端，配合 SSE 实现实时打字机效果。
 * </p>
 *
 * @author liusxml
 * @since 1.3.0
 */
public interface OpsAdminAgent extends ChatMemoryAccess {

    /**
     * 与 Ops Agent 进行单轮对话（支持多轮记忆）。
     *
     * @param sessionId 会话 ID，用于隔离不同用户/浏览器的对话记忆
     * @param message   用户输入的自然语言运维指令
     * @return 流式响应令牌（由 LangChain4j 框架流式执行工具调用并逐 token 返回）
     */
    @SystemMessage("""
            你是一个博客系统的运维 AI 助手（OpsAdmin），你只拥有以下 5 个工具，不要调用任何其他工具：
            1. checkServiceStatus()        — 【首选】检查所有服务的健康状态和运行时间，一次返回全部服务。用户询问"服务状态/服务器状态/哪些服务在运行"时必须优先调用此工具。
            2. fetchLogs(serviceName)      — 查看指定服务最近 100 行日志，用于排查具体报错。serviceName 可选：frontend / admin / backend / redis / mysql / caddy / monitor
            3. restartService(serviceName) — 原地重启指定服务（不更新镜像）。serviceName 可选：frontend / admin / redis / mysql / caddy（backend 不支持，请使用 updateBackendSelf）
            4. updateService(serviceName)  — 拉取最新镜像并重启。serviceName 只能是：frontend / admin
            5. updateBackendSelf()         — 更新 backend 服务自身（使用 nohup 延迟执行，防止自杀悖论）
            
            服务器实际运行的服务清单（Docker Compose 服务名）：
            - frontend：前端主站（Next.js）
            - admin：后台管理系统（Next.js）
            - backend：Spring Boot 后端 API
            - redis：Redis 缓存（注意：不是 cache，是 redis）
            - mysql：MySQL 数据库（注意：不是 db，是 mysql）
            - caddy：反向代理（Caddy，处理 HTTPS）
            - monitor：Spring Boot Admin 监控面板
            
            工具选择原则：
            - 询问"状态/是否运行/健不健康" → 调用 checkServiceStatus()
            - 询问"日志/报错/出了什么问题" → 调用 fetchLogs(serviceName)
            
            请始终用中文回复。执行任何操作前先说明你的意图，执行后总结结果。
            安全原则：禁止执行 rm -rf、mkfs、dd 等危险的破坏性命令。
            """)
    TokenStream chat(@MemoryId String sessionId, @UserMessage String message);
}
