package com.blog.article.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.article.domain.entity.ArticleTagEntity;
import com.blog.article.dto.TagDTO;
import com.blog.article.service.ITagService;
import com.blog.article.vo.TagVO;
import com.blog.common.exception.SystemErrorCode;
import com.blog.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * 标签管理控制器
 *
 * @author liusxml
 * @since 1.7.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/tags")
@RequiredArgsConstructor
@Tag(name = "标签管理", description = "标签管理相关接口")
public class TagAdminController {

    private final ITagService tagService;

    /**
     * 分页查询标签列表
     */
    @GetMapping
    @Operation(summary = "分页查询标签", description = "分页查询标签列表，支持按名称搜索")
    public Result<IPage<TagVO>> listTags(
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "标签名称（模糊搜索）") @RequestParam(required = false) String name) {
        log.info("分页查询标签: current={}, size={}, name={}", current, size, name);

        Page<ArticleTagEntity> page = new Page<>(current, size);
        var queryWrapper = Wrappers.lambdaQuery(ArticleTagEntity.class)
                .like(StringUtils.isNotBlank(name), ArticleTagEntity::getName, name)
                .orderByDesc(ArticleTagEntity::getArticleCount)
                .orderByDesc(ArticleTagEntity::getCreateTime);

        IPage<TagVO> result = tagService.pageVo(page, queryWrapper);
        return Result.success(result);
    }

    /**
     * 获取标签详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取标签详情", description = "根据ID获取标签详细信息")
    public Result<TagVO> getTag(
            @Parameter(description = "标签ID") @PathVariable Long id) {
        log.info("获取标签详情: id={}", id);
        Optional<TagVO> tagOpt = tagService.getVoById(id);
        return tagOpt.map(Result::success)
                .orElseGet(() -> Result.error(SystemErrorCode.NOT_FOUND, "标签不存在"));
    }

    /**
     * 创建标签
     */
    @PostMapping
    @Operation(summary = "创建标签", description = "创建新的标签")
    public Result<Serializable> createTag(@RequestBody TagDTO dto) {
        log.info("创建标签: name={}", dto.getName());
        Serializable id = tagService.saveByDto(dto);
        return Result.success(id);
    }

    /**
     * 批量创建标签
     */
    @PostMapping("/batch")
    @Operation(summary = "批量创建标签", description = "根据标签名称列表批量创建标签")
    public Result<List<Long>> batchCreateTags(@RequestBody List<String> names) {
        log.info("批量创建标签: count={}", names.size());
        List<Long> ids = tagService.batchCreate(names);
        return Result.success(ids);
    }

    /**
     * 更新标签
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新标签", description = "更新标签信息")
    public Result<Void> updateTag(
            @Parameter(description = "标签ID") @PathVariable Long id,
            @RequestBody TagDTO dto) {
        log.info("更新标签: id={}, name={}", id, dto.getName());
        dto.setId(id);
        boolean success = tagService.updateByDto(dto);
        return success ? Result.success() : Result.error(SystemErrorCode.OPERATION_FAILED, "更新失败");
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签", description = "删除指定标签")
    public Result<Void> deleteTag(
            @Parameter(description = "标签ID") @PathVariable Long id) {
        log.info("删除标签: id={}", id);
        boolean success = tagService.removeById(id);
        return success ? Result.success() : Result.error(SystemErrorCode.OPERATION_FAILED, "删除失败");
    }

    /**
     * 合并标签
     */
    @PostMapping("/{id}/merge")
    @Operation(summary = "合并标签", description = "将源标签合并到目标标签")
    public Result<Void> mergeTags(
            @Parameter(description = "源标签ID") @PathVariable Long id,
            @Parameter(description = "目标标签ID") @RequestParam Long targetTagId) {
        log.info("合并标签: sourceId={}, targetId={}", id, targetTagId);
        tagService.mergeTags(id, targetTagId);
        return Result.success();
    }
}
