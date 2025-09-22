package com.blog.frameworktest.impl;

import com.blog.common.base.BaseServiceImpl;
import com.blog.frameworktest.ITestService;
import com.blog.frameworktest.TestConverter;
import com.blog.frameworktest.TestDTO;
import com.blog.frameworktest.TestEntity;
import com.blog.frameworktest.mapper.TestMapper;
import com.blog.frameworktest.TestVO;
import org.springframework.stereotype.Service;

@Service
public class TestServiceImpl extends BaseServiceImpl<TestMapper, TestEntity, TestVO, TestDTO, TestConverter> implements ITestService {
    // 构造函数注入，完全遵循我们的最佳实践
    public TestServiceImpl(TestConverter converter) {
        super(converter);
    }
}