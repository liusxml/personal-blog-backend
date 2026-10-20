package com.blog.mcp.tools;

import com.blog.mcp.client.BlogAuthTokenManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 博客文章写入与发布 MCP 工具集
 *
 * <p><b>核心作用</b>：</p>
 * <ul>
 *   <li>向外部 AI 客户端暴露创建博客文章草稿、直接发布文章以及发布指定草稿的能力。</li>
 *   <li>内部通过 {@link BlogAuthTokenManager} 自动附加管理员 JWT Token，调用 {@code /api/v1/admin/articles} 接口。</li>
 * </ul>
 *
 * <p><b>业务流转设计</b>：</p>
 * <ul>
 *   <li>后端创建文章接口（{@code POST /api/v1/admin/articles}）默认将文章保存为<b>草稿状态</b>，并返回新文章的 {@code articleId}。</li>
 *   <li>若用户在对话中要求"直接发布"（{@code publishNow = true}），本工具会在拿到 {@code articleId} 后，
 *       自动串联调用 {@code POST /api/v1/admin/articles/{id}/publish} 完成一键发布与 Qdrant 向量化。</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlogArticleWriteTools {

    private final RestClient blogRestClient;
    private final BlogAuthTokenManager authTokenManager;
    private final ObjectMapper objectMapper;

    /**
     * 创建新的博客文章（支持仅保存为草稿或立即发布）
     *
     * @param title      文章标题（必填）
     * @param content    Markdown 格式的文章正文（必填）
     * @param summary    文章摘要（可选，不填则由后端自动提取）
     * @param categoryId 分类 ID（建议先通过 listCategories 工具查询）
     * @param tagIds     标签 ID 列表（建议先通过 listTags 工具查询）
     * @param publishNow 是否立即发布为公开文章（true=创建后立即发布，false=仅存为草稿）
     * @return 创建/发布结果说明
     */
    @Tool(description = "创建一篇新的个人博客文章（支持 Markdown 正文）。调用前建议先用 listCategories 和 listTags 查询可用的分类 ID 与标签 ID。可通过 publishNow 参数控制是仅保存为草稿 (false) 还是立即公开发布 (true)。")
    public String createArticle(
            @ToolParam(description = "文章标题（必填，不超过 255 字符）") String title,
            @ToolParam(description = "文章正文内容（必填，使用标准 Markdown 格式编写）") String content,
            @ToolParam(description = "文章简短摘要（可选，不超过 500 字符）", required = false) String summary,
            @ToolParam(description = "所属分类 ID（数字类型，可通过 listCategories 查询）", required = false) Long categoryId,
            @ToolParam(description = "关联的标签 ID 列表（数字数组，可通过 listTags 查询）", required = false) List<Long> tagIds,
            @ToolParam(description = "是否立即发布：true 表示创建后直接公开发布，false 或留空表示仅保存为草稿", required = false) Boolean publishNow) {

        log.info("MCP Tool 调用: createArticle(title={}, categoryId={}, tagIds={}, publishNow={})",
                title, categoryId, tagIds, publishNow);

        try {
            // 1. 组装符合后端 ArticleDTO 契约的请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("title", title);
            requestBody.put("content", content);
            // 默认设为原创文章 (type = 1)
            requestBody.put("type", 1);

            if (StringUtils.hasText(summary)) {
                requestBody.put("summary", summary.trim());
            }
            if (categoryId != null) {
                requestBody.put("categoryId", categoryId);
            }
            if (tagIds != null && !tagIds.isEmpty()) {
                requestBody.put("tagIds", tagIds);
            }

            // 2. 获取管理员 Bearer Token 并调用 POST /api/v1/admin/articles 创建草稿
            String bearerToken = authTokenManager.getBearerToken();
            String createResponse = blogRestClient.post()
                    .uri("/api/v1/admin/articles")
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(createResponse);
            if (root.path("code").asInt(-1) != 200) {
                return "创建文章失败，后端返回: " + createResponse;
            }

            long articleId = root.path("data").asLong();

            // 3. 如果需要立即发布，接着调用发布接口 POST /api/v1/admin/articles/{id}/publish
            if (Boolean.TRUE.equals(publishNow)) {
                String publishResponse = publishDraftArticle(articleId);
                return String.format("文章《%s》已创建并成功发布！文章 ID: %d，发布响应: %s",
                        title, articleId, publishResponse);
            }

            return String.format("文章《%s》已成功保存为草稿！文章 ID: %d（后续可调用 publishDraftArticle 工具将其正式发布）。",
                    title, articleId);

        } catch (Exception e) {
            log.error("MCP createArticle 执行失败", e);
            return "创建文章异常: " + e.getMessage();
        }
    }

    /**
     * 将已存在的草稿文章正式发布
     *
     * @param articleId 草稿文章 ID
     * @return 发布响应 JSON 字符串
     */
    @Tool(description = "根据文章 ID 将草稿状态的文章正式发布为公开可见状态，并自动触发 AI 向量化检索同步。")
    public String publishDraftArticle(
            @ToolParam(description = "待发布的草稿文章 ID（数字类型）") Long articleId) {

        log.info("MCP Tool 调用: publishDraftArticle(articleId={})", articleId);

        try {
            String bearerToken = authTokenManager.getBearerToken();
            return blogRestClient.post()
                    .uri("/api/v1/admin/articles/{id}/publish", articleId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("MCP publishDraftArticle 执行失败: articleId={}", articleId, e);
            return "发布文章异常: " + e.getMessage();
        }
    }
}
