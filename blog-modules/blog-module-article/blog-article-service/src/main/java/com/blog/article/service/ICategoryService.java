package com.blog.article.service;

import com.blog.article.domain.entity.ArticleCategoryEntity;
import com.blog.article.dto.CategoryDTO;
import com.blog.article.vo.CategoryTreeVO;
import com.blog.article.vo.CategoryVO;
import com.blog.common.base.IBaseService;

import java.util.List;
import java.util.Optional;

/**
 * 分类服务接口
 *
 * @author liusxml
 * @since 1.7.0
 */
public interface ICategoryService extends IBaseService<ArticleCategoryEntity, CategoryVO, CategoryDTO> {

    /**
     * 获取分类树
     *
     * @return 分类树列表
     */
    List<CategoryTreeVO> getCategoryTree();

    /**
     * 移动分类
     *
     * @param id           分类ID
     * @param newParentId  新父分类ID
     * @param newSortOrder 新排序权重
     */
    void moveCategory(Long id, Long newParentId, Integer newSortOrder);

    /**
     * 获取所有分类（扁平列表）
     *
     * @return 所有分类列表
     */
    List<CategoryVO> getAllCategories();

    /**
     * 根据 slug 获取分类
     *
     * @param slug 分类 slug
     * @return 分类VO
     */
    Optional<CategoryVO> getBySlug(String slug);
}
