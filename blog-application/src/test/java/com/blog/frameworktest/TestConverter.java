package com.blog.frameworktest;

import com.blog.common.base.BaseConverter;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TestConverter extends BaseConverter<TestDTO, TestEntity, TestVO> {
}