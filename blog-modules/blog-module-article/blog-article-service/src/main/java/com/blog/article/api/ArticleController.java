package com.blog.article.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.article.service.impl.ArticleServiceImpl;
import com.blog.common.exception.SystemErrorCode;
import com.blog.common.model.Result;
import com.blog.vo.ArticleDetailVO;
import com.blog.vo.ArticleListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
 * @author blog-system
 * @since 1.1.0
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
     * @param current    当前页码（从1开始）
     * @param size       每页大小
     * @param categoryId 分类ID（可选）
     * @param tagId      标签ID（可选）
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "获取文章列表", description = "分页查询已发布的文章列表")
    public Result<Page<ArticleListVO>> listArticles(
            @Parameter(description = "当前页码", example = "1") @RequestParam(defaultValue = "1") Long current,

            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Long size,

            @Parameter(description = "分类ID（可选）") @RequestParam(required = false) Long categoryId,

            @Parameter(description = "标签ID（可选）") @RequestParam(required = false) Long tagId) {

        log.info("查询文章列表: current={}, size={}, categoryId={}, tagId={}",
                current, size, categoryId, tagId);

        // TODO: 实现分页查询逻辑
        // 1. 构建查询条件（status=PUBLISHED, 分类、标签筛选）
        // 2. 调用 MyBatis-Plus 分页方法
        // 3. Entity -> VO 转换

        Page<ArticleListVO> pageResult = new Page<>(current, size);

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
