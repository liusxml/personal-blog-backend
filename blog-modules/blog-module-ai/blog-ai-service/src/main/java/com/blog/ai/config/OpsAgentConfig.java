package com.blog.ai.config;

import com.blog.ai.application.agent.OpsAdminAgent;
import com.blog.ai.application.tools.RemoteDevOpsTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ops Agent 装配配置
 *
 * <p>
 * 使用 LangChain4j {@link AiServices} 将以下组件组装为一个完整的 AI Agent：
 * </p>
 * <ul>
 *   <li>{@link StreamingChatModel}：由 DashScope Spring Boot Starter 自动注册（qwen-plus）</li>
 *   <li>{@link RemoteDevOpsTools}：提供 4 个 {@code @Tool} 方法（SSH 运维工具集）</li>
 *   <li>{@link MessageWindowChatMemory}：滑动窗口式对话记忆（每个 sessionId 独立隔离）</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.3.0
 */
@Configuration
@RequiredArgsConstructor
public class OpsAgentConfig {

    private final StreamingChatModel streamingChatModel;
    private final RemoteDevOpsTools remoteDevOpsTools;

    /**
     * 对话记忆窗口大小：保留最近 10 条消息（约 3~4 轮工具调用）。
     *
     * <p>
     * 为什么是 10（从 20 降低）：
     * <ul>
     * <li>DashScope 社区适配器 {@code QwenHelper.sanitizeMessages()} 存在 bug：
     *     当窗口截断导致孤立的 {@code ToolExecutionResultMessage} 出现时会直接 throw，
     *     而不是按 LangChain4j 标准行为静默丢弃。</li>
     * <li>Ops Agent 是单管理员工具，每次运维指令是独立任务，不需要超长上下文。</li>
     * <li>缩小窗口减少 tool pair 被截断到边界的概率，Controller 层还有 evictChatMemory + 重试双重保底。</li>
     * </ul>
     * </p>
     */
    private static final int MEMORY_WINDOW_SIZE = 10;

    /**
     * 构建并注册 {@link OpsAdminAgent} Bean。
     *
     * <p>
     * {@code chatMemoryProvider} 按 {@code @MemoryId}（即 sessionId）为每个会话
     * 独立创建一个 {@link MessageWindowChatMemory} 实例，确保多用户对话互不干扰。
     * </p>
     *
     * @return 由 LangChain4j 动态代理实现的 OpsAdminAgent
     */
    @Bean
    public OpsAdminAgent opsAdminAgent() {
        return AiServices.builder(OpsAdminAgent.class)
                .streamingChatModel(streamingChatModel)
                .tools(remoteDevOpsTools)
                .chatMemoryProvider(sessionId ->
                        MessageWindowChatMemory.builder()
                                .maxMessages(MEMORY_WINDOW_SIZE)
                                // 官方修复方案：确保 System 消息始终保留在消息列表头部
                                // 防止窗口截断导致 DashScope 收到的第一条非 system/user 消息而失败
                                .alwaysKeepSystemMessageFirst(true)
                                .build())
                .build();
    }
}
