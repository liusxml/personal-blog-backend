package com.blog.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.file.entity.FileEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件 Mapper
 *
 * <p>
 * 继承 MyBatis-Plus BaseMapper，自动提供 CRUD 方法。
 * </p>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Mapper
public interface FileMapper extends BaseMapper<FileEntity> {
    // MyBatis-Plus 已提供基础 CRUD 方法
    // 如需自定义 SQL，可在对应的 mapper.xml 中编写
}
