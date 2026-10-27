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
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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
@Configuration(proxyBeanMethods = false)
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
     * 自定义 Google GenAI {@link Client} Bean，默认使用 Gemini Developer API 模式（{@code vertexAI(false)}）。
     *
     * <p><b>为什么注入 {@link Environment} 而不是使用 {@code @Value}？</b><br>
     * Spring AI 的 {@code CachedContentServiceCondition} 会在配置类解析阶段（{@code BeanDefinitionRegistryPostProcessor} 阶段）
     * 提前触发 {@code GoogleGenAiChatModel} 与本 {@link Client} Bean 的实例化，此时负责解析 {@code @Value("${...}")}
     * 的 {@code PropertySourcesPlaceholderConfigurer} 尚未执行，会导致 {@code @Value} 注入未解析的字面量字符串。
     * 直接通过 {@link Environment#getProperty(String, String)} 读取可确保在容器任意生命周期阶段均能正确解析环境变量。</p>
     *
     * @param environment Spring 环境上下文（直接解析 {@code spring.ai.google.genai.api-key} 与 {@code GEMINI_API_KEY}）
     * @return 构建完成的 Google GenAI Client 实例
     */
    @Bean
    public Client genAiClient(Environment environment) {
        String apiKey = StringUtils.trimToEmpty(
                environment.getProperty("spring.ai.google.genai.api-key", "gemini-dev-placeholder"));
        Client.Builder builder = Client.builder().apiKey(apiKey);
        // 不要因为 AQ. 前缀开启 vertexAI(true)，AI Studio 新版密钥同样以 AQ. 开头但仅限 generativelanguage.googleapis.com
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
     * @param environment      Spring 环境上下文（读取 {@code ops.mcp-chat.mcp-token}）
     * @param jwtTokenProvider JWT 令牌签发器
     * @return HTTP 请求定制器
     */
    @Bean
    public McpSyncHttpClientRequestCustomizer opsMcpHttpClientRequestCustomizer(
            Environment environment,
            JwtTokenProvider jwtTokenProvider) {

        String configuredMcpToken = environment.getProperty("ops.mcp-chat.mcp-token", "");
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
