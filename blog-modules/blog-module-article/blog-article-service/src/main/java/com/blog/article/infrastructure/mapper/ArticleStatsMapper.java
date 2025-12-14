package com.blog.article.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.article.domain.entity.ArticleStatsEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章统计 Mapper
 *
 * @author liusxml
 * @since 1.1.0
 */
@Mapper
public interface ArticleStatsMapper extends BaseMapper<ArticleStatsEntity> {
}
