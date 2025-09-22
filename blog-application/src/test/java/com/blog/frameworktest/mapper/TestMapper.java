package com.blog.frameworktest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.frameworktest.TestEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface TestMapper extends BaseMapper<TestEntity> {

}