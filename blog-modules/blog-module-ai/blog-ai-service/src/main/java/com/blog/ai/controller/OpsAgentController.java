package com.blog.ai.controller;

import com.blog.ai.application.agent.OpsAdminAgent;
import com.blog.ai.infrastructure.ssh.SseEmitterContext;
import dev.langchain4j.service.TokenStream;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * Ops AI Agent 流式对话接口
 *
 * <p>
 * <b>注意：此接口不遵循项目统一 {@code Result<T>} 响应规范（特殊豁免）。</b><br>
 * 按照 HTTP/SSE 协议规定，{@code text/event-stream} 响应必须是原始事件流格式，
 * 若包装为 {@code Result<SseEmitter>} 则浏览器的 {@code EventSource} 无法解析。
 * </p>
 *
 * <p>SSE 事件说明：</p>
 * <ul>
 *   <li>{@code event: message} — AI 回复的流式 token（前端逐字追加）</li>
 *   <li>{@code event: ops_log} — SSH 命令的实时终端输出（由 SseOutputStream 发出）</li>
 *   <li>{@code event: tool_call} — 工具调用完成通知（含工具名和执行结果摘要）</li>
 *   <li>{@code event: done}     — 整轮对话结束信号（前端可关闭 EventSource）</li>
 *   <li>{@code event: error}    — 异常通知</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.3.0
 */
@Slf4j
@Tag(name = "Ops Agent", description = "AI 驱动的远程运维 Agent（管理员专用）")
@RestController
@RequestMapping("/api/v1/ops")
@RequiredArgsConstructor
public class OpsAgentController {

    /** SSE 连接超时时间：10 分钟（docker compose pull 等耗时操作可能需要较长时间） */
    private static final long SSE_TIMEOUT_MS = 10 * 60 * 1000L;

    private final OpsAdminAgent opsAdminAgent;

    /**
     * 与 Ops Agent 进行一次流式对话。
     *
     * <p>前端通过 {@code EventSource} 订阅此接口，接收实时 AI 回复与 SSH 终端日志。</p>
     *
     * @param message   用户输入的自然语言运维指令（如"帮我重启 frontend 服务"）
     * @param sessionId 会话 ID（用于维持多轮对话记忆，建议用 UUID 或用户ID）
     * @return SSE 流（text/event-stream），不包装 Result<T>，遵循 SSE 协议
     */
    @Operation(summary = "Ops Agent 流式对话", description = "管理员发送运维指令，AI 自动调用 SSH 工具并实时推送执行日志")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String sessionId) {

        if (StringUtils.isBlank(message)) {
            SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
            sendErrorAndComplete(emitter, "message 参数不能为空");
            return emitter;
        }

        log.info("Ops Agent chat: sessionId=[{}] message=[{}]", sessionId, message);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        SseEmitterContext.put(sessionId, emitter);

        // 注册 SSE 生命周期回调（防止连接断开后资源泄漏）
        emitter.onTimeout(() -> {
            log.warn("Ops SSE 超时: sessionId=[{}]", sessionId);
            SseEmitterContext.remove(sessionId);
        });
        emitter.onError(e -> {
            log.warn("Ops SSE 错误: sessionId=[{}] error=[{}]", sessionId, e.getMessage());
            SseEmitterContext.remove(sessionId);
        });
        emitter.onCompletion(() -> SseEmitterContext.remove(sessionId));

        // 构建并启动 LangChain4j TokenStream
        // 防御性处理：DashScope QwenHelper.sanitizeMessages() 在多轮工具调用后
        // 可能因消息窗口截断导致 "The first message should be a system/user message" 异常
        try {
            startTokenStream(opsAdminAgent.chat(sessionId, message), emitter, sessionId);
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("first message should be")) {
                log.warn("Ops Agent 消息窗口溢出，清空会话记忆后重试: sessionId=[{}]", sessionId);
                opsAdminAgent.evictChatMemory(sessionId);
                try {
                    startTokenStream(opsAdminAgent.chat(sessionId, message), emitter, sessionId);
                } catch (Exception retryEx) {
                    log.error("Ops Agent 重试仍失败: sessionId=[{}]", sessionId, retryEx);
                    sendErrorAndComplete(emitter, "对话记忆异常，已重置会话，请重新提问: " + retryEx.getMessage());
                }
            } else {
                log.error("Ops Agent 参数异常: sessionId=[{}]", sessionId, e);
                sendErrorAndComplete(emitter, e.getMessage());
            }
        }

        return emitter;
    }

    /**
     * 启动 TokenStream 并注册 SSE 事件回调。
     */
    private void startTokenStream(TokenStream tokenStream, SseEmitter emitter, String sessionId) {
        tokenStream
                .onPartialResponse(token -> sendEvent(emitter, "message", token))
                .onToolExecuted(toolExecution ->
                        sendEvent(emitter, "tool_call", toolExecution.request().name() + " 执行完成"))
                .onCompleteResponse(response -> {
                    sendEvent(emitter, "done", "DONE");
                    emitter.complete();
                })
                .onError(e -> {
                    log.error("Ops Agent 执行异常: sessionId=[{}]", sessionId, e);
                    sendErrorAndComplete(emitter, e.getMessage());
                })
                .start();
    }

    // ── 内部工具方法 ──────────────────────────────────────────────────────────

    /**
     * 向 SSE 发射器推送一条具名事件。
     * 推送失败（前端已断开）时仅记录 warn 日志，不抛出异常。
     */
    private void sendEvent(SseEmitter emitter, String eventName, String data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException | IllegalStateException e) {
            log.warn("Ops SSE 推送失败 [event:{}]: {}", eventName, e.getMessage());
        }
    }

    /**
     * 推送错误事件并关闭 SSE 连接。
     */
    private void sendErrorAndComplete(SseEmitter emitter, String errorMsg) {
        try {
            emitter.send(SseEmitter.event().name("error").data(errorMsg));
        } catch (Exception ignored) {
            // 连接已断开，忽略推送失败
        } finally {
            emitter.complete();
        }
    }
}
