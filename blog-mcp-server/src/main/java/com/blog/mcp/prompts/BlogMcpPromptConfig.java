package com.blog.mcp.prompts;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.ai.mcp.annotation.spring.SyncMcpAnnotationProviders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 博客 MCP Server 提示词模板 (Prompts) 声明式配置
 *
 * <p><b>核心升级（编程式 ➔ 声明式 {@code @McpPrompt} + {@code @McpArg} 注解）</b>：</p>
 * <ul>
 *   <li>使用 {@link McpPrompt} 与 {@link McpArg} 直接在 Java 方法签名上声明模板名称、描述与参数元数据，
 *       告别手动构建 {@code new McpSchema.PromptArgument(...)} 与 {@code Map.get(...)} 强转；</li>
 *   <li>通过 {@link SyncMcpAnnotationProviders#promptSpecifications(List)} 显式将注解方法解析为
 *       {@code List<SyncPromptSpecification>}，兼顾零启动警告与声明式开发体验。</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@SuppressWarnings("unused")
@Configuration(proxyBeanMethods = false)
public class BlogMcpPromptConfig {

    /**
     * 显式注册当前类中所有标记了 {@code @McpPrompt} 的提示词模板
     *
     * @return 同步提示词规范列表
     */
    @Bean
    public List<McpServerFeatures.SyncPromptSpecification> blogMcpPrompts() {
        return SyncMcpAnnotationProviders.promptSpecifications(List.of(this));
    }

    /**
     * 模板 1：技术博客标准创作工作流 (write-tech-article)
     */
    @McpPrompt(
            name = "write-tech-article",
            description = "撰写并保存个人博客技术文章的标准工作流模板（自动匹配分类与标签规范）"
    )
    public McpSchema.GetPromptResult writeTechArticle(
            @McpArg(
                    name = "topic",
                    description = "文章核心主题或标题方向（例如：Spring AI 1.1.8 实现 MCP Server 实战）",
                    required = true
            ) String topic,
            @McpArg(
                    name = "publishNow",
                    description = "是否直接公开发布（填 true 或 false，默认 false 仅存为草稿）"
            ) String publishNow) {

        String resolvedTopic = StringUtils.hasText(topic) ? topic.trim() : "未指定技术主题";
        boolean shouldPublish = StringUtils.hasText(publishNow) && "true".equalsIgnoreCase(publishNow.trim());

        log.info("MCP @McpPrompt 渲染技术文章创作模板: topic={}, publishNow={}", resolvedTopic, shouldPublish);

        String promptText = """
                请围绕主题【%s】为我的个人博客撰写一篇高质量的技术文章，并严格按以下步骤执行：
                
                1. 【查询分类与标签字典】
                   请先读取资源 `blog://catalog/taxonomy`（或调用 `listCategories` 与 `listTags` 工具），找出与该主题最匹配的 1 个 `categoryId` 和 1~3 个 `tagIds`。
                
                2. 【内容与排版规范】
                   - 标题 (`title`)：简洁有吸引力，不超过 60 字；
                   - 摘要 (`summary`)：提炼文章核心收获，控制在 120~200 字以内；
                   - 正文 (`content`)：采用标准 Markdown 编写，包含清晰的二级/三级标题 (`##` / `###`)、核心原理解析、代码示例以及总结。
                
                3. 【保存文章】
                   调用 `createArticle` 工具提交文章，其中 `publishNow` 参数请设置为 `%s`。
                   完成后请向我汇报新建的文章 ID、所选分类及标签名称。
                """.formatted(resolvedTopic, shouldPublish);

        return buildUserPromptResult(
                "围绕【" + resolvedTopic + "】撰写个人博客技术文章的标准工作流指令",
                promptText
        );
    }

    /**
     * 模板 2：服务器与容器全量运维巡检 SOP (ops-full-inspection)
     */
    @McpPrompt(
            name = "ops-full-inspection",
            description = "对个人博客后端服务与远程 VPS 服务器执行全量健康巡检并输出诊断报告"
    )
    public McpSchema.GetPromptResult opsFullInspection(
            @McpArg(
                    name = "focusService",
                    description = "可选：重点排查日志的容器服务名（可选值：frontend, admin, backend, redis, mysql, caddy, monitor, grafana, prometheus, alloy, dozzle, mysql-exporter, redis-exporter）"
            ) String focusService) {

        String resolvedService = StringUtils.hasText(focusService) ? focusService.trim() : "";
        log.info("MCP @McpPrompt 渲染服务器全量巡检模板: focusService={}", resolvedService);

        String logStep = StringUtils.hasText(resolvedService)
                ? "4. 调用 `fetchServiceLogs` 工具（参数 `serviceName = \"%s\"`），检查该容器最近是否有 ERROR 或异常堆栈。".formatted(resolvedService)
                : "4. 若在容器列表中发现状态异常的容器，请调用 `fetchServiceLogs` 工具拉取其尾部日志排查原因。";

        String promptText = """
                请对我的个人博客系统执行一次完整的生产级健康巡检，按顺序执行以下工具调用：
                
                1. 调用 `checkBackendHealth` 检查博客后端 (8080) 的 Spring Boot Actuator 健康状态（数据库、Redis、磁盘）；
                2. 调用 `checkServerResources` 通过 SSH 检查远程 VPS (107.175.36.50) 的 CPU 负载、内存占用率与磁盘剩余空间；
                3. 调用 `checkDockerContainers` 检查远程 VPS 上 Docker Compose 各容器的运行与健康状态；
                %s
                
                最后，请以清晰的 Markdown 表格形式汇总巡检结果，并给出是否存在风险的明确结论。
                """.formatted(logStep);

        return buildUserPromptResult("个人博客系统与远程 VPS 全量运维巡检 SOP 指令", promptText);
    }

    private McpSchema.GetPromptResult buildUserPromptResult(String description, String promptText) {
        McpSchema.PromptMessage userMessage = new McpSchema.PromptMessage(
                McpSchema.Role.USER,
                new McpSchema.TextContent(promptText)
        );
        return new McpSchema.GetPromptResult(description, List.of(userMessage));
    }
}
