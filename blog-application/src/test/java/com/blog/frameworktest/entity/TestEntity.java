package com.blog.frameworktest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("test_entity") // MyBatis-Plus 注解
public class TestEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
}
