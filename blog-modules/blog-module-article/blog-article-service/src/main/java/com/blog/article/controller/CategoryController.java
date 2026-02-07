package com.blog.article.controller;

import com.blog.article.service.ICategoryService;
import com.blog.article.vo.CategoryTreeVO;
import com.blog.article.vo.CategoryVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类控制器（用户端）
 *
 * <p>
 * 提供公开的分类查询接口，供前端展示使用。
 * </p>
 *
 * @author liusxml
 * @since 1.8.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "分类管理（用户端）", description = "公开的分类查询接口")
public class CategoryController {

    private final ICategoryService categoryService;

    /**
     * 获取分类树
     *
     * @return 完整分类树
     */
    @GetMapping("/tree")
    @Operation(summary = "获取分类树", description = "获取完整的分类树结构，用于导航展示")
    public Result<List<CategoryTreeVO>> getCategoryTree() {
        log.info("公开接口：获取分类树");
        List<CategoryTreeVO> tree = categoryService.getCategoryTree();
        return Result.success(tree);
    }

    /**
     * 获取扁平分类列表
     *
     * @return 所有分类（扁平）
     */
    @GetMapping
    @Operation(summary = "获取分类列表", description = "获取扁平的分类列表")
    public Result<List<CategoryVO>> listCategories() {
        log.info("公开接口：获取扁平分类列表");
        List<CategoryVO> categories = categoryService.getAllCategories();
        return Result.success(categories);
    }

    /**
     * 根据 Slug 获取分类
     *
     * @param slug 分类 slug
     * @return 分类详情
     */
    @GetMapping("/slug/{slug}")
    @Operation(summary = "根据 Slug 获取分类", description = "通过 URL slug 获取分类详情")
    public Result<CategoryVO> getCategoryBySlug(
            @Parameter(description = "分类 slug", example = "java-programming") @PathVariable String slug) {
        log.info("公开接口：根据 slug 获取分类: slug={}", slug);

        CategoryVO category = categoryService.getBySlug(slug)
                .orElseThrow(() -> new BusinessException(SystemErrorCode.NOT_FOUND, "分类不存在"));

        return Result.success(category);
    }
}
