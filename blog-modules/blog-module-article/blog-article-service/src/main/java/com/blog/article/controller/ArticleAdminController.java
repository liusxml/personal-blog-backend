package com.blog.article.controller;

import com.blog.article.api.dto.ArticleDTO;
import com.blog.article.api.dto.ArticleQueryDTO;
import com.blog.article.api.vo.ArticleDetailVO;
import com.blog.article.api.vo.ArticleListVO;
import com.blog.article.service.impl.ArticleServiceImpl;
import com.blog.common.exception.SystemErrorCode;
import com.blog.common.model.PageResult;
import com.blog.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * 文章管理控制器（管理端）
 *
 * <p>
 * 提供文章的创建、编辑、发布、归档、删除等管理功能。
 * </p>
 *
 * <p>
 * 权限要求：
 * </p>
 * <ul>
 * <li>需要 JWT 认证</li>
 * <li>部分操作需要作者权限验证</li>
 * </ul>
 *
 * <p>
 * 接口列表：
 * </p>
 * <ul>
 * <li>POST /api/v1/admin/articles - 创建文章（草稿）</li>
 * <li>PUT /api/v1/admin/articles/{id} - 更新文章</li>
 * <li>POST /api/v1/admin/articles/{id}/publish - 发布文章</li>
 * <li>POST /api/v1/admin/articles/{id}/archive - 归档文章</li>
 * <li>POST /api/v1/admin/articles/{id}/unarchive - 恢复归档</li>
 * <li>DELETE /api/v1/admin/articles/{id} - 删除文章</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/articles")
@RequiredArgsConstructor
@Tag(name = "文章管理（管理端）", description = "文章的创建、编辑、发布、删除等管理功能")
public class ArticleAdminController {

    private final ArticleServiceImpl articleService;

    /**
     * 分页查询文章列表（管理端）
     *
     * <p>
     * 与用户端接口的区别：
     * </p>
     * <ul>
     * <li>支持查询所有状态的文章（status=null 时）</li>
     * <li>包括草稿、已发布、已归档</li>
     * </ul>
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "分页查询文章列表", description = "管理端查询文章列表，支持所有状态")
    public Result<PageResult<ArticleListVO>> listArticles(ArticleQueryDTO query) {
        log.info("管理端查询文章列表: current={}, size={}, status={}, keyword={}",
                query.getCurrent(), query.getSize(), query.getStatus(), query.getKeyword());

        // 调用管理端专用的查询方法
        PageResult<ArticleListVO> pageResult = articleService.pageListForAdmin(query);

        return Result.success(pageResult);
    }

    /**
     * 创建文章（草稿状态）
     *
     * <p>
     * 新创建的文章默认为草稿状态，需要发布后才公开可见。
     * </p>
     *
     * @param dto 文章DTO
     * @return 文章ID
     */
    @PostMapping
    @Operation(summary = "创建文章", description = "创建新文章（默认草稿状态）")
    public Result<Long> createArticle(@Valid @RequestBody ArticleDTO dto) {
        log.info("创建文章: title={}", dto.getTitle());

        // 调用 BaseServiceImpl 的 saveByDto 方法
        Long articleId = (Long) articleService.saveByDto(dto);

        return Result.success(articleId);
    }

    /**
     * 更新文章
     *
     * <p>
     * 支持部分字段更新（MapStruct IGNORE 策略）。
     * </p>
     *
     * @param id  文章ID
     * @param dto 文章DTO
     * @return 成功标志
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新文章", description = "更新文章内容（支持部分更新）")
    public Result<Void> updateArticle(
            @Parameter(description = "文章ID", required = true) @PathVariable Long id,

            @Valid @RequestBody ArticleDTO dto) {

        log.info("更新文章: id={}, title={}", id, dto.getTitle());

        // 确保 DTO 中包含 ID
        dto.setId(id);

        // 调用 BaseServiceImpl 的 updateByDto 方法（安全更新）
        articleService.updateByDto(dto);

        return Result.success();
    }

    /**
     * 发布文章
     *
     * <p>
     * 状态流转：草稿 → 已发布
     * </p>
     *
     * <p>
     * 副作用：
     * </p>
     * <ul>
     * <li>设置发布时间</li>
     * <li>触发 ArticlePublishedEvent</li>
     * <li>初始化统计数据</li>
     * <li>异步生成向量</li>
     * </ul>
     *
     * @param id 文章ID
     * @return 成功标志
     */
    @PostMapping("/{id}/publish")
    @Operation(summary = "发布文章", description = "将草稿状态的文章发布为公开可见")
    public Result<Void> publishArticle(
            @Parameter(description = "文章ID", required = true) @PathVariable Long id) {

        log.info("发布文章: id={}", id);

        try {
            articleService.publishArticle(id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(SystemErrorCode.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error(SystemErrorCode.PARAM_ERROR, e.getMessage());
        }
    }

    /**
     * 归档文章
     *
     * <p>
     * 状态流转：已发布 → 已归档
     * </p>
     *
     * <p>
     * 归档后的文章不再显示在列表中，但仍可通过直接链接访问。
     * </p>
     *
     * @param id 文章ID
     * @return 成功标志
     */
    @PostMapping("/{id}/archive")
    @Operation(summary = "归档文章", description = "将已发布的文章归档")
    public Result<Void> archiveArticle(
            @Parameter(description = "文章ID", required = true) @PathVariable Long id) {

        log.info("归档文章: id={}", id);

        try {
            articleService.archiveArticle(id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(SystemErrorCode.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error(SystemErrorCode.PARAM_ERROR, e.getMessage());
        }
    }

    /**
     * 恢复归档文章
     *
     * <p>
     * 状态流转：已归档 → 已发布
     * </p>
     *
     * @param id 文章ID
     * @return 成功标志
     */
    @PostMapping("/{id}/unarchive")
    @Operation(summary = "恢复归档文章", description = "将已归档的文章恢复为已发布状态")
    public Result<Void> unarchiveArticle(
            @Parameter(description = "文章ID", required = true) @PathVariable Long id) {

        log.info("恢复归档文章: id={}", id);

        try {
            articleService.unarchiveArticle(id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(SystemErrorCode.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error(SystemErrorCode.PARAM_ERROR, e.getMessage());
        }
    }

    /**
     * 删除文章
     *
     * <p>
     * 逻辑删除，设置 is_deleted = 1。
     * </p>
     *
     * @param id 文章ID
     * @return 成功标志
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除文章", description = "逻辑删除文章")
    public Result<Void> deleteArticle(
            @Parameter(description = "文章ID", required = true) @PathVariable Long id) {

        log.info("删除文章: id={}", id);

        // 调用 BaseServiceImpl 的 removeById 方法
        boolean success = articleService.removeById(id);

        return success ? Result.success() : Result.error(SystemErrorCode.SYSTEM_ERROR);
    }

    /**
     * 获取文章详情（管理端）
     *
     * <p>
     * 与用户端不同，管理端可以查看草稿状态的文章。
     * </p>
     *
     * @param id 文章ID
     * @return 文章详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取文章详情", description = "获取文章详情（包括草稿）")
    public Result<ArticleDetailVO> getArticleById(
            @Parameter(description = "文章ID", required = true) @PathVariable Long id) {

        log.info("查询文章详情（管理端）: id={}", id);

        Optional<ArticleDetailVO> vo = articleService.getVoById(id);

        return vo.map(Result::success)
                .orElseGet(() -> Result.error(SystemErrorCode.NOT_FOUND, "文章不存在"));
    }
}
