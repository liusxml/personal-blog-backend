package com.blog.mcp.resources;

import com.blog.mcp.tools.BlogArticleReadTools;
import com.blog.mcp.tools.BlogServerOpsTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 博客 MCP Server 只读资源 (Resources) 注册配置
 *
 * <p><b>什么是 MCP Resource？</b><br>
 * 与由大模型主动触发的 {@code @Tool} 不同，{@code Resource} 是通过唯一 URI（如 {@code blog://catalog/taxonomy}）
 * 暴露给 AI 客户端或用户的<b>只读数据快照</b>（类似给大模型挂载本地参考文档）。</p>
 *
 * <p><b>为什么需要它？</b><br>
 * 比如 AI 在写技术文章前需要知道所有可用的分类与标签 ID。通过直接挂载或读取 {@code blog://catalog/taxonomy} 资源，
 * 客户端可以一次性获得全量分类+标签字典，无需大模型额外耗费两轮 Tool Call 推理。</p>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BlogMcpResourceConfig {

    private static final String TAXONOMY_URI = "blog://catalog/taxonomy";
    private static final String HEALTH_URI = "blog://system/health";

    private final BlogArticleReadTools articleReadTools;
    private final BlogServerOpsTools serverOpsTools;
    private final ObjectMapper objectMapper;

    /**
     * 注册博客系统的同步只读 MCP 资源列表
     *
     * <p>Spring AI 的 {@code McpServerAutoConfiguration} 会自动扫描容器中类型为
     * {@code List<McpServerFeatures.SyncResourceSpecification>} 的 Bean 并注册到 MCP Server。</p>
     *
     * @return 同步资源规范列表
     */
    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> blogMcpResources() {
        return List.of(
                buildTaxonomyResource(),
                buildSystemHealthResource()
        );
    }

    /**
     * 资源 1：博客全部分类与标签字典快照 (blog://catalog/taxonomy)
     */
    private McpServerFeatures.SyncResourceSpecification buildTaxonomyResource() {
        McpSchema.Resource resource = McpSchema.Resource.builder()
                .uri(TAXONOMY_URI)
                .name("博客分类与标签速查表")
                .description("一次性返回个人博客的全部分类树 (categories) 与全部标签列表 (tags)，供撰写文章时直接查阅 categoryId 与 tagIds。")
                .mimeType("application/json")
                .build();

        return new McpServerFeatures.SyncResourceSpecification(resource, (exchange, request) -> {
            log.info("MCP Resource 读取分类与标签字典快照: uri={}", request.uri());
            try {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("snapshotTime", LocalDateTime.now().toString());
                payload.put("categories", objectMapper.readTree(articleReadTools.listCategories()).path("data"));
                payload.put("tags", objectMapper.readTree(articleReadTools.listTags()).path("data"));

                String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
                return new McpSchema.ReadResourceResult(
                        List.of(new McpSchema.TextResourceContents(request.uri(), "application/json", json))
                );
            } catch (Exception e) {
                log.error("生成博客分类与标签资源快照失败", e);
                return new McpSchema.ReadResourceResult(
                        List.of(new McpSchema.TextResourceContents(request.uri(), "text/plain",
                                "读取分类与标签快照失败: " + e.getMessage()))
                );
            }
        });
    }

    /**
     * 资源 2：博客后端实时健康状态快照 (blog://system/health)
     */
    private McpServerFeatures.SyncResourceSpecification buildSystemHealthResource() {
        McpSchema.Resource resource = McpSchema.Resource.builder()
                .uri(HEALTH_URI)
                .name("博客后端健康状态快照")
                .description("实时读取个人博客后端 (8080) 的 Spring Boot Actuator 健康检查状态 (MySQL / Redis / Disk)。")
                .mimeType("application/json")
                .build();

        return new McpServerFeatures.SyncResourceSpecification(resource, (exchange, request) -> {
            log.info("MCP Resource 读取后端健康状态快照: uri={}", request.uri());
            String healthJson = serverOpsTools.checkBackendHealth();
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(request.uri(), "application/json", healthJson))
            );
        });
    }
}
