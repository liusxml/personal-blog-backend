package com.blog.ai.config;

import com.blog.common.security.JwtTokenProvider;
import com.google.genai.Client;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

/**
 * Ops Copilot — Spring AI + MCP Client 模式装配配置类
 *
 * <p><b>是什么</b>：<br>
 * 基于 Spring AI 1.1.x 官方体系（{@link ChatClient} + Advisor 责任链 + {@link ChatMemory} + MCP Client）
 * 构建的第二套运维 AI 助手链路，与 {@link OpsAgentConfig} 中的 LangChain4j 模式并存对比。</p>
 *
 * <p><b>有什么作用</b>：</p>
 * <ol>
 *   <li>适配 Google Cloud Vertex AI Express Mode API Key（{@code AQ.} 前缀自动开启 {@code vertexAI(true)}）。</li>
 *   <li>通过 {@link McpSyncHttpClientRequestCustomizer} 为 MCP Streamable HTTP 客户端注入管理员 Bearer Token。</li>
 *   <li>通过懒加载 {@link ToolCallbackProvider} 在首次对话时才执行 MCP {@code initialize()} 握手，避免同进程启动期死锁。</li>
 *   <li>挂载 {@link MessageChatMemoryAdvisor}（滑动窗口记忆）与 {@link SimpleLoggerAdvisor}（请求/响应可观测性审计）。</li>
 * </ol>
 *
 * <p><b>为什么这么设计</b>：<br>
 * 将工具发现与执行彻底解耦到标准 MCP 协议层（{@code /mcp}），大模型宿主侧仅依赖 {@link McpSyncClient}，
 * 未来若将 {@code blog-mcp-server} 拆分为独立微服务，仅需修改 {@code application.yaml} 中的连接地址，业务代码零改动。</p>
 *
 * @author liusxml
 * @since 1.4.0
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "ops.mcp-chat", name = "enabled", havingValue = "true")
public class OpsMcpChatConfig {

    /**
     * 对话记忆滑动窗口大小：保留最近 10 条消息（与 LangChain4j 模式的 MEMORY_WINDOW_SIZE 保持对齐）
     */
    private static final int MEMORY_WINDOW_SIZE = 10;

    /**
     * 内部回环自动签发 MCP 短期管理员 Token 的有效时长（5 分钟，单位毫秒）
     */
    private static final long INTERNAL_MCP_TOKEN_TTL_MILLIS = 5 * 60 * 1000L;

    /**
     * Spring AI + MCP 模式的系统提示词（精确对齐 blog-mcp-server 暴露的只读诊断与内容管理工具）
     */
    private static final String OPS_MCP_SYSTEM_PROMPT = """
            你是一个个人博客系统的运维与管理 AI 助手（Spring AI + MCP Client 模式）。
            你通过 MCP (Model Context Protocol) 协议连接本系统的 MCP Server，拥有以下核心工具：
            
            【一、服务器与容器四维只读巡检工具】
            1. checkBackendHealth()           — 检查博客后端 Spring Boot Actuator 健康状态（应用存活、MySQL、Redis 等指标）。
            2. checkServerResources()         — 通过 SSH 检查 VPS 服务器的系统运行时间、CPU 负载 (uptime)、内存占用 (free -h) 和磁盘空间 (df -h)。
            3. checkDockerContainers()        — 通过 SSH 检查 VPS 上所有 Docker Compose 服务容器的运行状态与健康结果 (docker compose ps)。
            4. fetchServiceLogs(serviceName)  — 通过 SSH 查看指定 Docker 服务最近 50 行运行日志。
               serviceName 可选值（共 13 个）：frontend / admin / backend / redis / mysql / caddy / monitor / grafana / prometheus / alloy / dozzle / mysql-exporter / redis-exporter
            
            【二、博客内容查询工具】
            5. listCategories()               — 查询博客所有分类列表。
            6. listTags()                     — 查询博客所有标签列表。
            7. listArticles(...)              — 分页查询或搜索博客文章列表（支持按关键词、状态筛选）。
            8. getArticleDetail(articleId)    — 获取指定文章的完整详情与 Markdown 正文。
            
            工具调用原则：
            - 用户询问"服务状态/容器状态/哪些服务在运行" → 优先调用 checkDockerContainers() 或同时调用 checkBackendHealth()
            - 用户询问"服务器负载/CPU/内存/磁盘" → 调用 checkServerResources()
            - 用户询问"某个服务的日志/报错排查" → 调用 fetchServiceLogs(serviceName)
            
            请始终用中文回复，排版清晰美观。调用工具前简要说明你的诊断思路，调用完成后对结果进行结构化总结。
            """;

    /**
     * 自定义 Google GenAI {@link Client} Bean，自动兼容 Vertex AI Express Mode Key（{@code AQ.} 前缀）。
     *
     * <p><b>为什么需要自定义此 Bean？</b><br>
     * Spring AI 官方 {@code GoogleGenAiChatAutoConfiguration} 默认仅在配置了 {@code project-id} 和 {@code location}
     * 时才调用 {@code builder.vertexAI(true)}。而 Google Cloud Vertex AI Express Mode 签发的 {@code AQ.} 前缀 API Key
     * 只需 API Key 即可免 Project ID 调用，但底层 SDK 强制要求开启 {@code vertexAI(true)}。
     * 由于官方自动配置标记了 {@code @ConditionalOnMissingBean}，在此声明自定义 {@link Client} 即可无缝接管。</p>
     *
     * @param apiKey 从环境变量 {@code GEMINI_API_KEY} 注入的密钥
     * @return 构建完成的 Google GenAI Client 实例
     */
    @Bean
    public Client genAiClient(@Value("${spring.ai.google.genai.api-key}") String apiKey) {
        Client.Builder builder = Client.builder().apiKey(apiKey);
        if (apiKey != null && apiKey.startsWith("AQ.")) {
            log.info("检测到 Vertex AI Express Mode API Key (AQ. 前缀)，自动开启 Google GenAI Client vertexAI(true) 模式");
            builder.vertexAI(true);
        }
        return builder.build();
    }

    /**
     * 配置 MCP Streamable HTTP 客户端请求头定制器，自动为发往 {@code /mcp} 的请求附加管理员 Bearer Token。
     *
     * <p><b>工作原理與双层兜底设计</b>：</p>
     * <ol>
     *   <li>若环境变量显式配置了 {@code OPS_MCP_TOKEN}，直接使用该长效 Token。</li>
     *   <li>若未配置 {@code OPS_MCP_TOKEN}（如本地开箱即用场景），利用同进程的 {@link JwtTokenProvider}
     *       每次请求动态签发一个 5 分钟有效期的内部管理员 MCP Token（{@code ROLE_ADMIN} + {@code tokenType=MCP}），
     *       既严格走完 Spring Security {@code /mcp} 鉴权过滤链，又免去手动复制粘贴 Token 的繁琐步骤。</li>
     * </ol>
     *
     * @param configuredMcpToken 配置文件中注入的 {@code ops.mcp-chat.mcp-token}
     * @param jwtTokenProvider   JWT 令牌签发器
     * @return HTTP 请求定制器
     */
    @Bean
    public McpSyncHttpClientRequestCustomizer opsMcpHttpClientRequestCustomizer(
            @Value("${ops.mcp-chat.mcp-token:}") String configuredMcpToken,
            JwtTokenProvider jwtTokenProvider) {

        return (builder, method, endpoint, body, context) -> {
            String tokenToUse = configuredMcpToken;
            if (StringUtils.isBlank(tokenToUse)) {
                UserDetails internalAdmin = User.withUsername("ops-mcp-internal")
                        .password("")
                        .authorities("ROLE_ADMIN")
                        .build();
                tokenToUse = jwtTokenProvider.generateMcpToken(
                        internalAdmin, null, INTERNAL_MCP_TOKEN_TTL_MILLIS);
            }
            builder.header("Authorization", "Bearer " + tokenToUse);
        };
    }

    /**
     * 配置 Ops MCP Chat 专用的滑动窗口对话记忆仓库。
     *
     * <p><b>是什么</b>：Spring AI 1.x 标准 {@link ChatMemory} 接口的内存滑动窗口实现。<br>
     * <b>为什么是 10 条</b>：与 LangChain4j 模式保持一致，按 {@code sessionId} 隔离不同会话，
     * 保留最近 10 条消息（约 3~4 轮工具调用），兼顾多轮追问上下文与 Token 消耗控制。</p>
     *
     * @return 滑动窗口对话记忆实例
     */
    @Bean("opsMcpChatMemory")
    public ChatMemory opsMcpChatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(MEMORY_WINDOW_SIZE)
                .build();
    }

    /**
     * 装配 Ops Copilot 的 Spring AI {@link ChatClient} 核心 Bean。
     *
     * <p><b>核心架构亮点</b>：</p>
     * <ol>
     *   <li><b>Advisor 责任链（洋葱模型）</b>：
     *       <ul>
     *         <li>{@link MessageChatMemoryAdvisor}：在调用大模型前自动按 {@code ChatMemory.CONVERSATION_ID}
     *             加载历史消息注入 Prompt，并在流式响应结束后自动将 User/Assistant 消息写回记忆窗口。</li>
     *         <li>{@link SimpleLoggerAdvisor}：切面化记录标准化 Prompt 请求与模型响应日志，便于调试与可观测性审计。</li>
     *       </ul>
     *   </li>
     *   <li><b>同进程 MCP 懒初始化（Lazy Initialization）</b>：
     *       由于 MCP Client 连接的是本进程 {@code http://localhost:8080/mcp}，若在 Spring 容器启动阶段握手，
     *       嵌入式 Tomcat 尚未监听端口会导致连接死锁。因此配置 {@code initialized: false}，并通过自定义
     *       {@link ToolCallbackProvider} 在首次收到用户提问时才触发 {@link McpSyncClient#initialize()} 握手与工具发现。</li>
     * </ol>
     *
     * @param chatModel      Google GenAI 聊天模型（默认 gemini-2.5-flash，支持单次请求通过 Options 动态切换模型）
     * @param chatMemory     滑动窗口对话记忆
     * @param mcpSyncClients 由 Spring AI MCP Client Starter 自动创建的同步 MCP 客户端列表
     * @return 装配完成的 ChatClient 实例
     */
    @Bean("opsMcpChatClient")
    public ChatClient opsMcpChatClient(
            GoogleGenAiChatModel chatModel,
            @Qualifier("opsMcpChatMemory") ChatMemory chatMemory,
            List<McpSyncClient> mcpSyncClients) {

        // 懒加载包装器：首次获取工具列表时才执行 MCP initialize() 协议握手，彻底规避同进程启动期死锁
        // 使用 Spring AI 官方推荐的 Builder 模式（避免已废弃的直接 new 构造器）
        ToolCallbackProvider lazyMcpToolCallbackProvider = () -> {
            for (McpSyncClient client : mcpSyncClients) {
                if (!client.isInitialized()) {
                    log.info("首次触发 Spring AI + MCP 模式对话，开始向本地 /mcp 端点执行 MCP initialize() 握手...");
                    client.initialize();
                    log.info("MCP Client 握手成功，已完成远端 MCP 工具发现");
                }
            }
            return SyncMcpToolCallbackProvider.builder()
                    .mcpClients(mcpSyncClients)
                    .build()
                    .getToolCallbacks();
        };

        return ChatClient.builder(chatModel)
                .defaultSystem(OPS_MCP_SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor()
                )
                .defaultToolCallbacks(lazyMcpToolCallbackProvider)
                .build();
    }
}
