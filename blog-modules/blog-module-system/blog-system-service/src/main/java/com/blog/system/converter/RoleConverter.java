package com.blog.system.converter;

import com.blog.common.base.BaseConverter;
import com.blog.system.api.dto.RoleDTO;
import com.blog.system.api.vo.RoleVO;
import com.blog.system.entity.SysRole;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * 角色转换器
 *
 * @author liusxml
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RoleConverter extends BaseConverter<RoleDTO, SysRole, RoleVO> {
    // 继承 BaseConverter 的所有方法
}
