package com.blog.comment.infrastructure.converter;

import com.blog.comment.api.dto.CommentDTO;
import com.blog.comment.api.vo.CommentVO;
import com.blog.comment.domain.entity.CommentEntity;
import com.blog.common.base.BaseConverter;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * 评论转换器
 *
 * @author liusxml
 * @since 1.2.0
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CommentConverter extends BaseConverter<CommentDTO, CommentEntity, CommentVO> {

}
