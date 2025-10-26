package com.blog.frameworktest.converter;

import com.blog.common.base.BaseConverter;
import com.blog.frameworktest.dto.TestDTO;
import com.blog.frameworktest.entity.TestEntity;
import com.blog.frameworktest.vo.TestVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TestConverter extends BaseConverter<TestDTO, TestEntity, TestVO> {
}