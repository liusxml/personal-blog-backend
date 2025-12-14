package com.blog.article.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.article.domain.entity.ArticleTagEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章标签 Mapper
 *
 * @author blog-system
 * @since 1.1.0
 */
@Mapper
public interface ArticleTagMapper extends BaseMapper<ArticleTagEntity> {
}
