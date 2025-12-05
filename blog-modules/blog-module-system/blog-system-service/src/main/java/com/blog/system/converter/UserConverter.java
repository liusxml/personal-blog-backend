package com.blog.system.converter;

import com.blog.common.base.BaseConverter;
import com.blog.system.api.dto.UserDTO;
import com.blog.system.api.vo.UserVO;
import com.blog.system.entity.SysUser;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * 用户转换器
 *
 * @author liusxml
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserConverter extends BaseConverter<UserDTO, SysUser, UserVO> {
    // 继承 BaseConverter 的所有方法
    // toDto, toEntity, toVo, toDto集合, toEntity集合, toVo集合
}
