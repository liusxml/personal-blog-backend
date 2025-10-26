package com.blog.frameworktest.service;

import com.blog.common.base.IBaseService;
import com.blog.frameworktest.dto.TestDTO;
import com.blog.frameworktest.entity.TestEntity;
import com.blog.frameworktest.vo.TestVO;

public interface ITestService extends IBaseService<TestEntity, TestVO, TestDTO> {
}
