package com.blog.article.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.article.domain.entity.ArticleCategoryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章分类 Mapper
 *
 * @author liusxml
 * @since 1.1.0
 */
@Mapper
public interface ArticleCategoryMapper extends BaseMapper<ArticleCategoryEntity> {
}
