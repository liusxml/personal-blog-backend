package com.blog.article.controller;

import com.blog.article.api.dto.ArticleQueryDTO;
import com.blog.article.api.vo.ArticleDetailVO;
import com.blog.article.api.vo.ArticleListVO;
import com.blog.article.service.impl.ArticleServiceImpl;
import com.blog.common.exception.SystemErrorCode;
import com.blog.common.model.PageResult;
import com.blog.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * 文章控制器（用户端）
 *
 * <p>
 * 提供公开的文章查询接口，供前端展示使用。
 * </p>
 *
 * <p>
 * 遵循项目规范：
 * </p>
 * <ul>
 * <li>返回 Result&lt;T&gt; 统一响应</li>
 * <li>使用 @Tag 和 @Operation 注解（OpenAPI文档）</li>
 * <li>RESTful API 设计</li>
 * <li>使用 @RequiredArgsConstructor 构造注入</li>
 * </ul>
 *
 * <p>
 * 接口列表：
 * </p>
 * <ul>
 * <li>GET /api/v1/articles - 文章列表（分页）</li>
 * <li>GET /api/v1/articles/{id} - 文章详情</li>
 * <li>GET /api/v1/articles/{id}/related - 相关文章推荐</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/articles")
@RequiredArgsConstructor
@Tag(name = "文章管理（用户端）", description = "公开的文章查询接口")
public class ArticleController {

    private final ArticleServiceImpl articleService;

    /**
     * 获取文章列表（分页）
     *
     * <p>
     * 支持分页查询，默认按发布时间倒序。
     * </p>
     *
     * @param current 当前页码（从1开始）
     * @param size    每页大小
     * @param tagId   标签ID（可选）
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "获取文章列表", description = "分页查询已发布的文章列表")
    @ApiResponse(responseCode = "200", description = "查询成功")
    public Result<PageResult<ArticleListVO>> listArticles(ArticleQueryDTO query) {
        log.info("查询文章列表: current={}, size={}, categoryId={}, tagId={}",
                query.getCurrent(), query.getSize(), query.getCategoryId(), query.getTagId());

        // 调用 Service 层分页查询
        PageResult<ArticleListVO> pageResult = articleService.pageList(query);

        return Result.success(pageResult);
    }

    /**
     * 获取文章详情
     *
     * <p>
     * 返回文章完整内容，包括作者、分类、标签和统计数据。
     * </p>
     *
     * <p>
     * 副作用：
     * </p>
     * <ul>
     * <li>异步增加浏览量</li>
     * </ul>
     *
     * @param id       文章ID
     * @param password 访问密码（加密文章需要）
     * @return 文章详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取文章详情", description = "根据ID查询文章完整内容")
    public Result<ArticleDetailVO> getArticleById(
            @Parameter(description = "文章ID", required = true) @PathVariable Long id,

            @Parameter(description = "访问密码（加密文章需要）") @RequestParam(required = false) String password) {

        log.info("查询文章详情: id={}", id);

        // 检查访问权限
        boolean hasAccess = articleService.checkAccessPermission(id, password);
        if (!hasAccess) {
            return Result.error(SystemErrorCode.ACCESS_DENIED, "无权访问该文章");
        }

        // 获取文章详情
        Optional<ArticleDetailVO> vo = articleService.getVoById(id);

        if (vo.isEmpty()) {
            return Result.error(SystemErrorCode.NOT_FOUND, "文章不存在");
        }

        // 异步增加浏览量
        articleService.incrementViewCount(id);

        return Result.success(vo.get());
    }

    /**
     * 获取相关文章推荐
     *
     * <p>
     * 基于 MySQL 9.4 VECTOR 向量搜索，推荐语义相似的文章。
     * </p>
     *
     * @param id    当前文章ID
     * @param limit 推荐数量（默认10篇）
     * @return 相关文章列表
     */
    @GetMapping("/{id}/related")
    @Operation(summary = "获取相关文章推荐", description = "基于向量搜索推荐相似文章")
    public Result<List<ArticleListVO>> getRelatedArticles(
            @Parameter(description = "当前文章ID", required = true) @PathVariable Long id,

            @Parameter(description = "推荐数量", example = "10") @RequestParam(defaultValue = "10") Integer limit) {

        log.info("获取相关文章: id={}, limit={}", id, limit);

        List<ArticleListVO> relatedArticles = articleService.getRelatedArticles(id, limit);

        return Result.success(relatedArticles);
    }
}
