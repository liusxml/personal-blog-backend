package com.blog.article.controller;

import com.blog.article.dto.CategoryDTO;
import com.blog.article.service.ICategoryService;
import com.blog.article.vo.CategoryTreeVO;
import com.blog.article.vo.CategoryVO;
import com.blog.common.exception.SystemErrorCode;
import com.blog.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * 分类管理控制器
 *
 * @author liusxml
 * @since 1.7.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@Tag(name = "分类管理", description = "分类管理相关接口")
public class CategoryAdminController {

    private final ICategoryService categoryService;

    /**
     * 获取分类树
     */
    @GetMapping("/tree")
    @Operation(summary = "获取分类树", description = "获取完整的分类树结构")
    public Result<List<CategoryTreeVO>> getCategoryTree() {
        log.info("获取分类树");
        List<CategoryTreeVO> tree = categoryService.getCategoryTree();
        return Result.success(tree);
    }

    /**
     * 获取分类详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取分类详情", description = "根据ID获取分类详细信息")
    public Result<CategoryVO> getCategory(
            @Parameter(description = "分类ID") @PathVariable Long id) {
        log.info("获取分类详情: id={}", id);
        Optional<CategoryVO> categoryOpt = categoryService.getVoById(id);
        return categoryOpt.map(Result::success)
                .orElseGet(() -> Result.error(SystemErrorCode.NOT_FOUND, "分类不存在"));
    }

    /**
     * 创建分类
     */
    @PostMapping
    @Operation(summary = "创建分类", description = "创建新的文章分类")
    public Result<Serializable> createCategory(@RequestBody CategoryDTO dto) {
        log.info("创建分类: name={}, parentId={}", dto.getName(), dto.getParentId());
        Serializable id = categoryService.saveByDto(dto);
        return Result.success(id);
    }

    /**
     * 更新分类
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新分类", description = "更新分类信息")
    public Result<Void> updateCategory(
            @Parameter(description = "分类ID") @PathVariable Long id,
            @RequestBody CategoryDTO dto) {
        log.info("更新分类: id={}, name={}", id, dto.getName());
        dto.setId(id);
        boolean success = categoryService.updateByDto(dto);
        return success ? Result.success() : Result.error(SystemErrorCode.OPERATION_FAILED, "更新失败");
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类", description = "删除指定分类（如果有子分类则不能删除）")
    public Result<Void> deleteCategory(
            @Parameter(description = "分类ID") @PathVariable Long id) {
        log.info("删除分类: id={}", id);
        boolean success = categoryService.removeById(id);
        return success ? Result.success() : Result.error(SystemErrorCode.OPERATION_FAILED, "删除失败");
    }

    /**
     * 移动分类
     */
    @PutMapping("/{id}/move")
    @Operation(summary = "移动分类", description = "移动分类到新的父分类下或调整排序")
    public Result<Void> moveCategory(
            @Parameter(description = "分类ID") @PathVariable Long id,
            @Parameter(description = "新父分类ID") @RequestParam(required = false) Long newParentId,
            @Parameter(description = "新排序权重") @RequestParam(required = false) Integer newSortOrder) {
        log.info("移动分类: id={}, newParentId={}, newSortOrder={}", id, newParentId, newSortOrder);
        categoryService.moveCategory(id, newParentId, newSortOrder);
        return Result.success();
    }
}
