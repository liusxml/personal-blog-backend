package com.blog.mcp.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * 博客内容只读 MCP 工具集
 *
 * <p><b>核心作用</b>：</p>
 * <ul>
 *   <li>向外部 AI 客户端暴露查询博客分类、标签、文章列表与文章详情的只读能力。</li>
 *   <li>当用户让 AI "写一篇关于 xxx 的文章" 时，AI 会先调用本类中的 {@link #listCategories()} 和 {@link #listTags()}，
 *       自动获取合法的 {@code categoryId} 与 {@code tagIds}，再执行写文章操作。</li>
 * </ul>
 *
 * <p><b>技术原理</b>：</p>
 * <ul>
 *   <li>{@code @Tool}：Spring AI 会扫描该注解，将方法名和 {@code description} 注册为 MCP 工具描述。</li>
 *   <li>{@code @ToolParam}：Spring AI 通过反射读取参数类型与描述，自动生成 JSON Schema 供大模型传参。</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlogArticleReadTools {

    /**
     * 注入配置好 base-url (http://localhost:8080) 的 Spring 6 RestClient
     */
    private final RestClient blogRestClient;

    /**
     * 获取博客所有文章分类列表
     *
     * <p>为什么需要这个工具？<br>
     * 写文章接口需要传入数字类型的 {@code categoryId}，大模型无法凭空猜测数据库里的分类 ID，
     * 因此通过此工具让大模型先查出完整的分类清单（含 id、name、slug）。</p>
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
     * 分页搜索已发布的博客文章列表
     *
     * @param keyword 搜索关键词（可选，匹配文章标题或摘要）
     * @param current 页码（可选，默认第 1 页）
     * @param size    每页条数（可选，默认 10 条）
     * @return 分页文章列表 JSON 字符串
     */
    @Tool(description = "分页查询或按关键词搜索博客中已发布的文章列表（包含文章 ID、标题、摘要、分类、标签、浏览量与发布时间）。")
    public String searchArticles(
            @ToolParam(description = "搜索关键词，可留空查询最新文章", required = false) String keyword,
            @ToolParam(description = "当前页码，从 1 开始，默认 1", required = false) Integer current,
            @ToolParam(description = "每页条数，默认 10", required = false) Integer size) {

        int pageNum = (current != null && current > 0) ? current : 1;
        int pageSize = (size != null && size > 0) ? size : 10;

        log.info("MCP Tool 调用: searchArticles(keyword={}, current={}, size={})", keyword, pageNum, pageSize);

        return blogRestClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/v1/articles")
                            .queryParam("current", pageNum)
                            .queryParam("size", pageSize);
                    if (StringUtils.hasText(keyword)) {
                        uriBuilder.queryParam("keyword", keyword.trim());
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .body(String.class);
    }

    /**
     * 根据文章 ID 获取单篇文章的完整详情（含 Markdown 正文）
     *
     * @param articleId 文章主键 ID
     * @return 文章详情 JSON 字符串
     */
    @Tool(description = "根据文章 ID 获取单篇博客文章的完整详情（包含完整的 Markdown 正文内容、标题、分类和标签信息）。")
    public String getArticleDetail(
            @ToolParam(description = "文章 ID（数字类型主键）") Long articleId) {

        log.info("MCP Tool 调用: getArticleDetail(articleId={})", articleId);
        return blogRestClient.get()
                .uri("/api/v1/articles/{id}", articleId)
                .retrieve()
                .body(String.class);
    }
}
