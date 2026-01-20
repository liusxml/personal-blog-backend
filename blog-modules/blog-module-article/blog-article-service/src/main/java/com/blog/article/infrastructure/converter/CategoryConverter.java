package com.blog.article.infrastructure.converter;

import com.blog.article.domain.entity.ArticleCategoryEntity;
import com.blog.article.dto.CategoryDTO;
import com.blog.article.vo.CategoryTreeVO;
import com.blog.article.vo.CategoryVO;
import com.blog.common.base.BaseConverter;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * 分类转换器
 *
 * @author liusxml
 * @since 1.7.0
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CategoryConverter extends BaseConverter<CategoryDTO, ArticleCategoryEntity, CategoryVO> {

    /**
     * Entity -> TreeVO
     */
    CategoryTreeVO toTreeVO(ArticleCategoryEntity entity);
}
