package com.blog.mcp.resources;

import com.blog.mcp.tools.BlogArticleReadTools;
import com.blog.mcp.tools.BlogServerOpsTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springaicommunity.mcp.context.McpSyncRequestContext;
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
 * <p><b>核心能力</b>：</p>
 * <ul>
 *   <li>使用 {@link McpResource} 注解直接在普通方法上声明静态资源与动态 URI 模板资源；</li>
 *   <li>通过注入 {@link McpSyncRequestContext} 实现服务端向客户端双向通信（主动推送 {@code notifications/message} 实时日志，
 *       并可按需探测与调用客户端的 {@code roots}、{@code elicit}、{@code sample} 反向能力）。</li>
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
            title = "博客分类与标签速查表",
            description = "一次性返回个人博客的全部分类树 (categories) 与全部标签列表 (tags)，供撰写文章时直接查阅 categoryId 与 tagIds。",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult readTaxonomy(McpSyncRequestContext context) {
        log.info("MCP @McpResource 读取分类与标签字典快照: uri={}", TAXONOMY_URI);
        context.info("正在从博客后端 (8080) 拉取全量分类树与标签字典快照...");
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("snapshotTime", LocalDateTime.now().toString());
            payload.put("categories", objectMapper.readTree(articleReadTools.listCategories()).path("data"));
            payload.put("tags", objectMapper.readTree(articleReadTools.listTags()).path("data"));

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            context.info("分类与标签字典快照读取完成: uri=" + TAXONOMY_URI);
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(TAXONOMY_URI, "application/json", json))
            );
        } catch (Exception e) {
            log.error("生成博客分类与标签资源快照失败", e);
            context.error("生成分类与标签资源快照失败: " + e.getMessage());
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(TAXONOMY_URI, "text/plain",
                            "读取分类与标签快照失败: " + e.getMessage()))
            );
        }
    }

    /**
     * 静态资源 2：博客后端实时健康状态快照 (blog://system/health)
     *
     * <p>同时演示通过 {@link McpSyncRequestContext} 探测当前连接的 MCP 客户端反向能力
     * （{@code rootsEnabled} / {@code elicitEnabled} / {@code sampleEnabled}）并实时推流日志到客户端。</p>
     */
    @McpResource(
            uri = HEALTH_URI,
            name = "博客后端健康状态快照",
            title = "博客后端健康状态快照",
            description = "实时读取个人博客后端 (8080) 的 Spring Boot Actuator 健康检查状态 (MySQL / Redis / Disk)。",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult readSystemHealth(McpSyncRequestContext context) {
        log.info("MCP @McpResource 读取后端健康状态快照: uri={}", HEALTH_URI);
        context.info("正在检查博客后端 /actuator/health 并探测客户端双向能力 [roots=%s, elicit=%s, sample=%s]..."
                .formatted(context.rootsEnabled(), context.elicitEnabled(), context.sampleEnabled()));

        String healthJson = serverOpsTools.checkBackendHealth();
        context.info("博客后端健康状态快照读取成功！");
        return new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(HEALTH_URI, "application/json", healthJson))
        );
    }

    /**
     * 动态资源模板 3：按文章 ID 直接挂载单篇文章或草稿详情 (blog://articles/{articleId})
     *
     * @param context   MCP 同步请求上下文（由框架自动注入，不计入 URI 变量）
     * @param articleId 19 位雪花文章 ID（字符串格式，自动从 URI 模板 {@code {articleId}} 绑定）
     * @return 文章完整详情 JSON 资源结果
     */
    @McpResource(
            uri = ARTICLE_TEMPLATE_URI,
            name = "按 ID 挂载单篇博客文章或草稿",
            title = "按 ID 挂载单篇博客文章或草稿",
            description = "通过 URI 模板 blog://articles/{articleId} 直接读取指定 19 位文章 ID 的完整 Markdown 正文与元数据（支持草稿与已发布文章）。",
            mimeType = "application/json"
    )
    public McpSchema.ReadResourceResult readArticleById(McpSyncRequestContext context, String articleId) {
        String resourceUri = "blog://articles/" + articleId;
        log.info("MCP @McpResource Template 读取单篇文章资源: uri={}", resourceUri);
        context.info("正在按 ID 加载博客文章或草稿详情: articleId=" + articleId);

        String articleJson = articleReadTools.getArticleDetail(articleId);
        return new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(resourceUri, "application/json", articleJson))
        );
    }
}
