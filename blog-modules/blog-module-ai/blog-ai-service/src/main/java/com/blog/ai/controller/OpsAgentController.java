package com.blog.ai.controller;

import com.blog.ai.application.agent.OpsAdminAgent;
import com.blog.ai.infrastructure.ssh.SseEmitterContext;
import com.blog.common.model.Result;
import com.google.genai.Client;
import com.google.genai.types.ListModelsConfig;
import com.google.genai.types.Model;
import dev.langchain4j.service.TokenStream;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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

    /**
     * SSE 连接超时时间：10 分钟（docker compose pull 等耗时操作可能需要较长时间）
     */
    private static final long SSE_TIMEOUT_MS = 10 * 60 * 1000L;

    /**
     * Spring AI + MCP 模式默认主力模型（固定置顶于模型下拉框首项）
     */
    private static final String DEFAULT_GEMINI_MODEL = "gemini-2.5-flash";

    /**
     * 旧模式（LangChain4j + 本地 @Tool 直接执行 SSH）运维 Agent
     */
    private final OpsAdminAgent opsAdminAgent;

    /**
     * 新模式（Spring AI ChatClient + MCP Client 协议回环调用 /mcp）延迟提供器。
     *
     * <p><b>为什么使用 {@link ObjectProvider} 而不是直接注入 {@link ChatClient}？</b><br>
     * 因为 {@code OpsMcpChatConfig} 受 {@code ops.mcp-chat.enabled} 配置项控制。
     * 使用 {@link ObjectProvider} 既遵循了项目强制的 {@code @RequiredArgsConstructor} 构造器注入规范，
     * 又能保证当配置开关设为 {@code false} 时应用依然能正常启动，不会抛出 {@code NoSuchBeanDefinitionException}。</p>
     */
    private final ObjectProvider<ChatClient> opsMcpChatClientProvider;

    /**
     * Google GenAI SDK 客户端延迟提供器（用于调用 {@code client.models.list()} 动态拉取可用模型列表）
     */
    private final ObjectProvider<Client> genAiClientProvider;

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
     * 获取 Spring AI + MCP 模式可用的 Gemini 模型列表。
     *
     * <p><b>是什么</b>：供管理后台 Ops Copilot 聊天窗口模型选择器调用的模型列表查询接口。<br>
     * <b>有什么作用</b>：始终将 {@code gemini-2.5-flash} 置顶为默认主力模型，随后调用 Google GenAI SDK 的
     * {@code client.models.list()} 动态获取当前 API Key 支持的所有 Gemini 对话模型。<br>
     * <b>为什么这样设计（双层兜底）</b>：当本地尚未配置有效 API Key，或使用 Vertex AI Express Mode Key
     * （无 Project ID 时 Google 服务端限制调用列举模型端点）导致远端拉取抛出异常时，自动回退到 Spring AI 官方内置的
     * {@link GoogleGenAiChatModel.ChatModel} 枚举列表，确保前端下拉框在任何环境下都有完整可选项。</p>
     *
     * @return 统一响应 Result，包含以 gemini-2.5-flash 置顶的去重模型 ID 列表
     */
    @Operation(
            summary = "获取 Spring AI + MCP 模式可用模型列表",
            description = "优先从 Google GenAI API 动态拉取可用 Gemini 模型列表，失败时自动回退到 Spring AI 内置模型枚举"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/mcp-models")
    public Result<List<String>> getMcpModels() {
        Set<String> models = new LinkedHashSet<>();
        // 1. 始终将 gemini-2.5-flash 固定排在第一位（默认 · 主力均衡）
        models.add(DEFAULT_GEMINI_MODEL);

        Client genAiClient = genAiClientProvider.getIfAvailable();
        if (genAiClient != null) {
            try {
                for (Model model : genAiClient.models.list(ListModelsConfig.builder().pageSize(100).build())) {
                    String rawName = model.name().orElse("");
                    String modelId = extractModelId(rawName);
                    if (isSupportedGeminiChatModel(modelId)) {
                        models.add(modelId);
                    }
                }
            } catch (Exception e) {
                log.warn("从 Google GenAI API 动态拉取模型列表失败，回退至 Spring AI 内置模型枚举: {}", e.getMessage());
            }
        }

        // 2. 若远端未返回额外模型，使用 Spring AI 官方内置的 Gemini 枚举兜底
        if (models.size() <= 1) {
            for (GoogleGenAiChatModel.ChatModel preset : GoogleGenAiChatModel.ChatModel.values()) {
                String modelId = preset.getValue();
                if (isSupportedGeminiChatModel(modelId)) {
                    models.add(modelId);
                }
            }
        }

        return Result.success(new ArrayList<>(models));
    }

    /**
     * 与 Ops Agent（Spring AI + MCP Client 模式）进行一次流式对话。
     *
     * <p><b>是什么</b>：基于 Spring AI {@link ChatClient} 与 MCP Client 协议驱动的流式运维问答端点。<br>
     * <b>有什么作用</b>：接收管理员指令与所选 Gemini 模型后，通过 Advisor 链加载滑动窗口对话记忆，
     * 自动向本服务 {@code /mcp} 端点发现并调用运维巡检/博客管理工具，并将最终总结通过 SSE 实时推送给前端。<br>
     * <b>为什么这样设计</b>：事件协议（{@code message / done / error}）与旧接口 {@code /chat} 完全对齐，
     * 同时利用 {@link GoogleGenAiChatOptions} 实现单次请求级别的运行时模型动态切换。</p>
     *
     * @param message        用户输入的自然语言指令（如"检查一下服务器状态"）
     * @param conversationId Spring AI 会话记忆 ID（优先使用，默认空）
     * @param sessionId      兼容前端 useOpsStream 默认传递的 sessionId 参数（当 conversationId 未传时兜底）
     * @param model          本次对话选用的 Gemini 模型 ID（默认 gemini-2.5-flash）
     * @return SSE 事件流（text/event-stream）
     */
    @Operation(
            summary = "Ops Agent 流式对话（Spring AI + MCP 模式）",
            description = "基于 Spring AI ChatClient + MCP Client 回环调用 /mcp 工具的流式对话接口，支持运行时动态切换 Gemini 模型"
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/mcp-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter mcpChat(
            @RequestParam String message,
            @RequestParam(defaultValue = "") String conversationId,
            @RequestParam(defaultValue = "default") String sessionId,
            @RequestParam(defaultValue = DEFAULT_GEMINI_MODEL) String model) {

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        if (StringUtils.isBlank(message)) {
            sendErrorAndComplete(emitter, "message 参数不能为空");
            return emitter;
        }

        ChatClient chatClient = opsMcpChatClientProvider.getIfAvailable();
        if (chatClient == null) {
            sendErrorAndComplete(emitter, "Spring AI + MCP 模式未启用（请检查 ops.mcp-chat.enabled 配置）");
            return emitter;
        }

        // 优先使用显式传入的 conversationId，未传时自动回退到前端通用的 sessionId
        String activeConversationId = StringUtils.defaultIfBlank(conversationId, sessionId);
        // 校验前端传入的模型名是否合法，不合法或为空时回退到默认 gemini-2.5-flash
        String activeModel = isSupportedGeminiChatModel(model) ? model : DEFAULT_GEMINI_MODEL;
        log.info("Ops MCP Agent chat: conversationId=[{}] model=[{}] message=[{}]",
                activeConversationId, activeModel, message);

        // 异步执行流式对话与 MCP 工具调用，确保主线程立即返回 SseEmitter 建立 SSE 通道，
        // 避免首次懒加载 MCP initialize() 握手或同步工具调用阻塞 Servlet 请求线程
        CompletableFuture.runAsync(() -> {
            try {
                chatClient.prompt()
                        .user(message)
                        .options(GoogleGenAiChatOptions.builder().model(activeModel).build())
                        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, activeConversationId))
                        .stream()
                        .content()
                        .subscribe(
                                token -> {
                                    if (StringUtils.isNotEmpty(token)) {
                                        sendEvent(emitter, "message", token);
                                    }
                                },
                                e -> {
                                    log.error("Ops MCP Agent 执行异常: conversationId=[{}]", activeConversationId, e);
                                    sendErrorAndComplete(emitter, e.getMessage());
                                },
                                () -> {
                                    sendEvent(emitter, "done", "DONE");
                                    emitter.complete();
                                }
                        );
            } catch (Exception e) {
                log.error("Ops MCP Agent 启动异常: conversationId=[{}]", activeConversationId, e);
                sendErrorAndComplete(emitter, e.getMessage());
            }
        });

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
     * 从 Google GenAI SDK 返回的模型资源路径（如 {@code models/gemini-2.5-flash} 或
     * {@code publishers/google/models/gemini-2.5-pro}）中提取纯模型 ID。
     */
    private String extractModelId(String rawName) {
        if (StringUtils.isBlank(rawName)) {
            return "";
        }
        int lastSlash = rawName.lastIndexOf('/');
        return lastSlash >= 0 ? rawName.substring(lastSlash + 1) : rawName;
    }

    /**
     * 校验是否为支持文本对话与工具调用的 Gemini 模型 ID（剔除 embedding、imagen、veo、tts、aqa 等非文本对话模型）。
     */
    private boolean isSupportedGeminiChatModel(String modelId) {
        if (StringUtils.isBlank(modelId) || !modelId.startsWith("gemini-")) {
            return false;
        }
        String lower = modelId.toLowerCase();
        return !lower.contains("embedding")
                && !lower.contains("imagen")
                && !lower.contains("veo")
                && !lower.contains("tts")
                && !lower.contains("audio")
                && !lower.contains("vision")
                && !lower.contains("aqa");
    }

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
