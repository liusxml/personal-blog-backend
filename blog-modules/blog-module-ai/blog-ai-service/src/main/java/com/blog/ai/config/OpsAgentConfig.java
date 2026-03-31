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
     * 对话记忆窗口大小：保留最近 20 条消息（约 6~8 轮对话）。
     *
     * <p>
     * 为什么是 20：每次 Tool 调用会产生 2~3 条消息（AiMessage + ToolExecutionResultMessage），
     * 10 条窗口在多工具调用场景下很快被填满导致截断；20 条可承载更多轮次。
     * </p>
     */
    private static final int MEMORY_WINDOW_SIZE = 20;

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
