package com.blog.frameworktest.service.impl;

import com.blog.common.base.BaseServiceImpl;
import com.blog.frameworktest.converter.TestConverter;
import com.blog.frameworktest.dto.TestDTO;
import com.blog.frameworktest.entity.TestEntity;
import com.blog.frameworktest.mapper.TestMapper;
import com.blog.frameworktest.service.ITestService;
import com.blog.frameworktest.vo.TestVO;
import org.springframework.stereotype.Service;

@Service
public class TestServiceImpl extends BaseServiceImpl<TestMapper, TestEntity, TestVO, TestDTO, TestConverter> implements ITestService {

    // 构造函数注入，完全遵循我们的最佳实践
    public TestServiceImpl(TestConverter converter) {
        super(converter);
    }
}