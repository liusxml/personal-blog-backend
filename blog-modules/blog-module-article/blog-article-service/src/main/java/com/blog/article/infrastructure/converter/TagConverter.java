package com.blog.article.infrastructure.converter;

import com.blog.article.domain.entity.ArticleTagEntity;
import com.blog.article.dto.TagDTO;
import com.blog.article.vo.TagVO;
import com.blog.common.base.BaseConverter;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * 标签转换器
 *
 * @author liusxml
 * @since 1.7.0
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TagConverter extends BaseConverter<TagDTO, ArticleTagEntity, TagVO> {
}
