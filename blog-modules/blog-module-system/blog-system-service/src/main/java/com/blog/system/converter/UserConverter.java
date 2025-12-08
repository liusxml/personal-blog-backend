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

    /**
     * 将 Entity 转换为 DTO（用于跨模块调用）
     *
     * @param entity Entity 对象
     * @return DTO 对象
     */
    UserDTO entityToDto(SysUser entity);
}
