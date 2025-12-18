package com.blog.comment.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.comment.domain.entity.CommentHistoryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论编辑历史 Mapper
 *
 * @author liusxml
 * @since 1.6.0
 */
@Mapper
public interface CommentHistoryMapper extends BaseMapper<CommentHistoryEntity> {
}
