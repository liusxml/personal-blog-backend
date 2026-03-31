package com.blog.ai.infrastructure.converter;

import com.blog.ai.api.dto.OpsCiEventDTO;
import com.blog.ai.api.vo.OpsCiEventVO;
import com.blog.ai.domain.entity.OpsCiEventEntity;
import com.blog.common.base.BaseConverter;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Ops CI 事件 MapStruct 转换器
 *
 * @author liusxml
 * @since 1.3.0
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface OpsCiEventConverter extends BaseConverter<OpsCiEventDTO, OpsCiEventEntity, OpsCiEventVO> {
    // 字段名一致，MapStruct 自动映射，无需手动配置
}
