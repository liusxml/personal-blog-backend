package com.blog.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.blog.article.infrastructure.converter.CategoryConverter;
import com.blog.article.domain.entity.ArticleCategoryEntity;
import com.blog.article.dto.CategoryDTO;
import com.blog.article.infrastructure.mapper.ArticleCategoryMapper;
import com.blog.article.service.ICategoryService;
import com.blog.article.vo.CategoryTreeVO;
import com.blog.article.vo.CategoryVO;
import com.blog.common.base.BaseServiceImpl;
import com.blog.common.exception.BusinessException;
import com.blog.common.exception.SystemErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 分类服务实现
 *
 * @author liusxml
 * @since 1.7.0
 */
@Slf4j
@Service
public class CategoryServiceImpl
        extends
        BaseServiceImpl<ArticleCategoryMapper, ArticleCategoryEntity, CategoryVO, CategoryDTO, CategoryConverter>
        implements ICategoryService {

    private final ArticleCategoryMapper categoryMapper;
    private final CategoryConverter categoryConverter;

    public CategoryServiceImpl(CategoryConverter converter, ArticleCategoryMapper mapper) {
        super(converter);
        this.categoryConverter = converter;
        this.categoryMapper = mapper;
    }

    @Override
    public List<CategoryTreeVO> getCategoryTree() {
        // 查询所有分类
        List<ArticleCategoryEntity> allCategories = categoryMapper.selectList(
                Wrappers.lambdaQuery(ArticleCategoryEntity.class)
                        .orderByAsc(ArticleCategoryEntity::getSortOrder));

        if (allCategories.isEmpty()) {
            return new ArrayList<>();
        }

        // 转换为VO
        List<CategoryTreeVO> allTreeVOs = allCategories.stream()
                .map(categoryConverter::toTreeVO)
                .collect(Collectors.toList());

        // 构建树形结构
        return buildTree(allTreeVOs, null);
    }

    /**
     * 构建树形结构
     *
     * @param all      所有节点
     * @param parentId 父节点ID
     * @return 树形结构
     */
    private List<CategoryTreeVO> buildTree(List<CategoryTreeVO> all, String parentId) {
        return all.stream()
                .filter(node -> Objects.equals(node.getParentId(), parentId))
                .peek(node -> node.setChildren(buildTree(all, node.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveCategory(Long id, Long newParentId, Integer newSortOrder) {
        ArticleCategoryEntity category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(SystemErrorCode.NOT_FOUND);
        }

        // 检查是否移动到自己的子分类
        if (newParentId != null && isDescendant(id, newParentId)) {
            throw new BusinessException(SystemErrorCode.PARAM_ERROR, "不能移动到自己的子分类下");
        }

        // 更新父分类和排序
        category.setParentId(newParentId);
        if (newSortOrder != null) {
            category.setSortOrder(newSortOrder);
        }

        // 更新path
        updatePathRecursively(category);

        categoryMapper.updateById(category);
    }

    /**
     * 判断targetId是否是categoryId的后代
     */
    private boolean isDescendant(Long categoryId, Long targetId) {
        ArticleCategoryEntity target = categoryMapper.selectById(targetId);
        if (target == null || target.getPath() == null) {
            return false;
        }
        return target.getPath().contains("/" + categoryId + "/");
    }

    /**
     * 递归更新分类路径
     */
    private void updatePathRecursively(ArticleCategoryEntity category) {
        // 计算新路径
        String newPath = calculatePath(category.getParentId(), category.getId());
        category.setPath(newPath);

        // 查询所有子分类
        List<ArticleCategoryEntity> children = categoryMapper.selectList(
                Wrappers.lambdaQuery(ArticleCategoryEntity.class)
                        .eq(ArticleCategoryEntity::getParentId, category.getId()));

        // 递归更新子分类
        for (ArticleCategoryEntity child : children) {
            child.setParentId(category.getId());
            updatePathRecursively(child);
            categoryMapper.updateById(child);
        }
    }

    /**
     * 计算分类路径
     */
    private String calculatePath(Long parentId, Long id) {
        if (parentId == null) {
            return "/" + id;
        }

        ArticleCategoryEntity parent = categoryMapper.selectById(parentId);
        if (parent == null || parent.getPath() == null) {
            return "/" + id;
        }

        return parent.getPath() + "/" + id;
    }

    @Override
    protected void preSave(ArticleCategoryEntity entity) {
        // 自动生成slug
        if (StringUtils.isBlank(entity.getSlug())) {
            entity.setSlug(generateSlug(entity.getName()));
        }

        // 初始化sortOrder
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }

        // 计算path
        entity.setPath(calculatePath(entity.getParentId(), entity.getId()));
    }

    @Override
    protected void preUpdate(ArticleCategoryEntity entity) {
        // 如果name变化了，更新slug
        ArticleCategoryEntity existing = categoryMapper.selectById(entity.getId());
        if (existing != null && !existing.getName().equals(entity.getName())) {
            if (StringUtils.isBlank(entity.getSlug())) {
                entity.setSlug(generateSlug(entity.getName()));
            }
        }
    }

    /**
     * 生成URL友好的slug
     */
    private String generateSlug(String name) {
        if (StringUtils.isBlank(name)) {
            return "";
        }

        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    @Override
    public List<CategoryVO> getAllCategories() {
        log.debug("获取所有分类（扁平列表）");
        List<ArticleCategoryEntity> categories = categoryMapper.selectList(
                Wrappers.lambdaQuery(ArticleCategoryEntity.class)
                        .orderByAsc(ArticleCategoryEntity::getSortOrder));

        return categories.stream()
                .map(converter::entityToVo)
                .collect(Collectors.toList());
    }

    @Override
    public java.util.Optional<CategoryVO> getBySlug(String slug) {
        log.debug("根据 slug 获取分类: slug={}", slug);

        if (StringUtils.isBlank(slug)) {
            return java.util.Optional.empty();
        }

        ArticleCategoryEntity entity = categoryMapper.selectOne(
                Wrappers.lambdaQuery(ArticleCategoryEntity.class)
                        .eq(ArticleCategoryEntity::getSlug, slug));

        if (entity == null) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(converter.entityToVo(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        // 检查是否有子分类
        Long childCount = categoryMapper.selectCount(
                Wrappers.lambdaQuery(ArticleCategoryEntity.class)
                        .eq(ArticleCategoryEntity::getParentId, id));

        if (childCount > 0) {
            throw new BusinessException(SystemErrorCode.PARAM_ERROR, "该分类下有子分类，无法删除");
        }

        // TODO: 检查是否有关联文章
        // 暂时允许删除

        return super.removeById(id);
    }
}
