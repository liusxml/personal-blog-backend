package com.blog.article.controller;

import com.blog.article.service.ITagService;
import com.blog.article.vo.TagVO;
import com.blog.common.exception.BusinessException;
import com.blog.common.exception.SystemErrorCode;
import com.blog.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签控制器（用户端）
 *
 * <p>
 * 提供公开的标签查询接口，供前端展示使用。
 * </p>
 *
 * @author liusxml
 * @since 1.8.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@Tag(name = "标签管理（用户端）", description = "公开的标签查询接口")
public class TagController {

    private final ITagService tagService;

    /**
     * 获取所有标签
     *
     * @param orderBy 排序方式 (article_count, create_time, name)
     * @param limit   限制数量
     * @return 标签列表
     */
    @GetMapping
    @Operation(summary = "获取标签列表", description = "获取所有标签，支持排序和数量限制")
    public Result<List<TagVO>> listTags(
            @Parameter(description = "排序方式", example = "article_count") @RequestParam(defaultValue = "article_count") String orderBy,

            @Parameter(description = "限制数量（可选）", example = "20") @RequestParam(required = false) Integer limit) {

        log.info("公开接口：获取标签列表: orderBy={}, limit={}", orderBy, limit);

        List<TagVO> tags = tagService.listTags(orderBy, limit);
        return Result.success(tags);
    }

    /**
     * 获取热门标签
     *
     * @param limit 数量限制
     * @return 热门标签列表
     */
    @GetMapping("/hot")
    @Operation(summary = "获取热门标签", description = "按文章数量倒序获取热门标签")
    public Result<List<TagVO>> getHotTags(
            @Parameter(description = "数量限制", example = "10") @RequestParam(defaultValue = "10") Integer limit) {

        log.info("公开接口：获取热门标签: limit={}", limit);

        List<TagVO> tags = tagService.listTags("article_count", limit);
        return Result.success(tags);
    }

    /**
     * 根据 Slug 获取标签
     *
     * @param slug 标签 slug
     * @return 标签详情
     */
    @GetMapping("/slug/{slug}")
    @Operation(summary = "根据 Slug 获取标签", description = "通过 URL slug 获取标签详情")
    public Result<TagVO> getTagBySlug(
            @Parameter(description = "标签 slug", example = "spring-boot") @PathVariable String slug) {

        log.info("公开接口：根据 slug 获取标签: slug={}", slug);

        TagVO tag = tagService.getBySlug(slug)
                .orElseThrow(() -> new BusinessException(SystemErrorCode.NOT_FOUND, "标签不存在"));

        return Result.success(tag);
    }
}
