package com.blog.mcp.resources;

import com.blog.mcp.tools.BlogArticleReadTools;
import com.blog.mcp.tools.BlogServerOpsTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.ai.mcp.annotation.spring.SyncMcpAnnotationProviders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 博客 MCP Server 只读资源 (Resources & Resource Templates) 声明式配置
 *
 * <p><b>核心升级（编程式 ➔ 声明式 {@code @McpResource} 注解）</b>：</p>
 * <ul>
 *   <li>使用 {@link McpResource} 注解直接在普通方法上声明静态资源与动态 URI 模板资源；</li>
 *   <li>通过 {@link SyncMcpAnnotationProviders} 显式将当前类上的 {@code @McpResource} 方法解析为
 *       {@code SyncResourceSpecification}（静态资源）和 {@code SyncResourceTemplateSpecification}（带 {@code {articleId}} 的动态资源模板），
 *       既保持了 {@code annotation-scanner.enabled=false} 的零警告极速启动，又享受了声明式注解的简洁语法。</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@SuppressWarnings("unused")
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class BlogMcpResourceConfig {

    private static final String TAXONOMY_URI = "blog://catalog/taxonomy";
    private static final String HEALTH_URI = "blog://system/health";
    private static final String ARTICLE_TEMPLATE_URI = "blog://articles/{articleId}";

    private final BlogArticleReadTools articleReadTools;
    private final BlogServerOpsTools serverOpsTools;
    private final ObjectMapper objectMapper;

    /**
     * 显式注册当前类中所有标记了 {@code @McpResource} 的静态资源（不含 URI 模板变量）
     */
    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> blogMcpResources() {
        return SyncMcpAnnotationProviders.resourceSpecifications(List.of(this));
    }

    /**
     * 显式注册当前类中所有标记了 {@code @McpResource} 的动态资源模板（包含 {@code {articleId}} 等 URI 模板变量）
     */
    @Bean
    public List<McpServerFeatures.SyncResourceTemplateSpecification> blogMcpResourceTemplates() {
        return SyncMcpAnnotationProviders.resourceTemplateSpecifications(List.of(this));
    }

    /**
     * 静态资源 1：博客全部分类与标签字典快照 (blog://catalog/taxonomy)
     */
    @McpResource(
            uri = TAXONOMY_URI,
            name = "博客分类与标签速查表",
            description = "一次性返回个人博客的全部分类树 (categories) 与全部标签列表 (tags)，供撰写文章时直接查阅 categoryId 与 tagIds。",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult readTaxonomy() {
        log.info("MCP @McpResource 读取分类与标签字典快照: uri={}", TAXONOMY_URI);
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("snapshotTime", LocalDateTime.now().toString());
            payload.put("categories", objectMapper.readTree(articleReadTools.listCategories()).path("data"));
            payload.put("tags", objectMapper.readTree(articleReadTools.listTags()).path("data"));

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(TAXONOMY_URI, "application/json", json))
            );
        } catch (Exception e) {
            log.error("生成博客分类与标签资源快照失败", e);
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(TAXONOMY_URI, "text/plain",
                            "读取分类与标签快照失败: " + e.getMessage()))
            );
        }
    }

    /**
     * 静态资源 2：博客后端实时健康状态快照 (blog://system/health)
     */
    @McpResource(
            uri = HEALTH_URI,
            name = "博客后端健康状态快照",
            description = "实时读取个人博客后端 (8080) 的 Spring Boot Actuator 健康检查状态 (MySQL / Redis / Disk)。",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult readSystemHealth() {
        log.info("MCP @McpResource 读取后端健康状态快照: uri={}", HEALTH_URI);
        String healthJson = serverOpsTools.checkBackendHealth();
        return new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(HEALTH_URI, "application/json", healthJson))
        );
    }

    /**
     * 动态资源模板 3：按文章 ID 直接挂载单篇文章或草稿详情 (blog://articles/{articleId})
     *
     * <p>由于 URI 中包含 {@code {articleId}} 占位符，MCP 协议会自动将其识别为 <b>Resource Template</b>，
     * 并自动将 URI 中的变量提取绑定到方法参数 {@code articleId} 上。</p>
     *
     * @param articleId 19 位雪花文章 ID（字符串格式）
     * @return 文章完整详情 JSON 资源结果
     */
    @McpResource(
            uri = ARTICLE_TEMPLATE_URI,
            name = "按 ID 挂载单篇博客文章或草稿",
            description = "通过 URI 模板 blog://articles/{articleId} 直接读取指定 19 位文章 ID 的完整 Markdown 正文与元数据（支持草稿与已发布文章）。",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult readArticleById(String articleId) {
        String resourceUri = "blog://articles/" + articleId;
        log.info("MCP @McpResource Template 读取单篇文章资源: uri={}", resourceUri);
        String articleJson = articleReadTools.getArticleDetail(articleId);
        return new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(resourceUri, "application/json", articleJson))
        );
    }
}
