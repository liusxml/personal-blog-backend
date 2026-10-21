package com.blog.mcp.tools;

import com.blog.mcp.client.BlogAuthTokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * 博客内容只读 MCP 工具集
 *
 * <p><b>核心作用</b>：</p>
 * <ul>
 *   <li>向外部 AI 客户端暴露查询博客分类、标签、文章列表（含草稿与已发布）与文章详情的只读能力。</li>
 *   <li>当用户让 AI "写一篇关于 xxx 的文章" 时，AI 可先调用本类中的 {@link #listCategories()} 和 {@link #listTags()}，
 *       或直接读取 {@code blog://catalog/taxonomy} 资源获取合法的 {@code categoryId} 与 {@code tagIds}。</li>
 * </ul>
 *
 * <p><b>工程细节（防雪花 ID 精度丢失）</b>：</p>
 * <ul>
 *   <li>后端文章主键采用 19 位雪花算法 ID（如 {@code 2112801352432652289}），超出 JavaScript {@code Number.MAX_SAFE_INTEGER}（16 位）。</li>
 *   <li>因此所有接收 {@code articleId} 的工具参数统一声明为 {@link String}（JSON Schema {@code "type": "string"}），彻底避免 JS/TS 客户端截断精度。</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlogArticleReadTools {

    private final RestClient blogRestClient;
    private final BlogAuthTokenManager authTokenManager;

    /**
     * 获取博客所有文章分类列表
     *
     * @return 分类列表 JSON 字符串
     */
    @Tool(description = "获取个人博客的所有文章分类列表（包含分类 ID、分类名称、slug）。在创建或发布文章前，请先调用此工具查询可用的分类 ID (categoryId)。")
    public String listCategories() {
        log.info("MCP Tool 调用: listCategories()");
        return blogRestClient.get()
                .uri("/api/v1/categories")
                .retrieve()
                .body(String.class);
    }

    /**
     * 获取博客所有文章标签列表
     *
     * @return 标签列表 JSON 字符串
     */
    @Tool(description = "获取个人博客的所有文章标签列表（包含标签 ID、标签名称、关联文章数）。在创建文章需要绑定标签时，请先调用此工具获取合法的标签 ID 列表 (tagIds)。")
    public String listTags() {
        log.info("MCP Tool 调用: listTags()");
        return blogRestClient.get()
                .uri("/api/v1/tags")
                .retrieve()
                .body(String.class);
    }

    /**
     * 分页搜索博客文章列表（支持查询草稿、已发布、已归档或全部状态）
     *
     * @param keyword 搜索关键词（可选，匹配文章标题或摘要）
     * @param status  文章状态过滤（可选：0=草稿，2=已发布，3=已归档，留空查询全部状态）
     * @param current 页码（可选，默认第 1 页）
     * @param size    每页条数（可选，默认 10 条）
     * @return 分页文章列表 JSON 字符串
     */
    @Tool(description = "分页查询或按关键词搜索博客文章列表（支持查询草稿 status=0、已发布 status=2、已归档 status=3，不传 status 则返回所有状态文章）。")
    public String searchArticles(
            @ToolParam(description = "搜索关键词，可留空查询最新文章", required = false) String keyword,
            @ToolParam(description = "文章状态过滤：0=草稿(DRAFT)，2=已发布(PUBLISHED)，3=已归档(ARCHIVED)，留空查询全部状态", required = false) Integer status,
            @ToolParam(description = "当前页码，从 1 开始，默认 1", required = false) Integer current,
            @ToolParam(description = "每页条数，默认 10", required = false) Integer size) {

        int pageNum = (current != null && current > 0) ? current : 1;
        int pageSize = (size != null && size > 0) ? size : 10;

        log.info("MCP Tool 调用: searchArticles(keyword={}, status={}, current={}, size={})",
                keyword, status, pageNum, pageSize);

        String bearerToken = authTokenManager.getBearerToken();
        return blogRestClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/v1/admin/articles")
                            .queryParam("current", pageNum)
                            .queryParam("size", pageSize);
                    if (status != null) {
                        uriBuilder.queryParam("status", status);
                    }
                    if (StringUtils.hasText(keyword)) {
                        uriBuilder.queryParam("keyword", keyword.trim());
                    }
                    return uriBuilder.build();
                })
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .body(String.class);
    }

    /**
     * 根据文章 ID 获取单篇文章的完整详情（含草稿或已发布文章的 Markdown 正文）
     *
     * @param articleId 文章主键 ID（字符串格式，防止 19 位雪花 ID 在 JS 客户端丢失精度）
     * @return 文章详情 JSON 字符串
     */
    @Tool(description = "根据文章 ID 获取单篇博客文章的完整详情（支持草稿与已发布文章，包含完整的 Markdown 正文内容、标题、分类和标签信息）。请以字符串形式传入 19 位文章 ID。")
    public String getArticleDetail(
            @ToolParam(description = "文章 ID（字符串格式，如 \"2112801352432652289\"，防止 64 位长整型精度丢失）") String articleId) {

        log.info("MCP Tool 调用: getArticleDetail(articleId={})", articleId);
        if (!StringUtils.hasText(articleId)) {
            return "参数错误：articleId 不能为空";
        }

        String bearerToken = authTokenManager.getBearerToken();
        return blogRestClient.get()
                .uri("/api/v1/admin/articles/{id}", articleId.trim())
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve()
                .body(String.class);
    }
}
