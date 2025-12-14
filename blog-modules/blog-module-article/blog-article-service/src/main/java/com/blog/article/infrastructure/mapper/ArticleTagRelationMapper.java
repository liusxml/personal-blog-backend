package com.blog.article.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.article.domain.entity.ArticleTagRelationEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章-标签关联 Mapper
 *
 * @author liusxml
 * @since 1.1.0
 */
@Mapper
public interface ArticleTagRelationMapper extends BaseMapper<ArticleTagRelationEntity> {
}
