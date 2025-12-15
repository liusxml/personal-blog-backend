package com.blog.comment.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.comment.domain.entity.CommentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论Mapper
 *
 * @author liusxml
 * @since 1.2.0
 */
@Mapper
public interface CommentMapper extends BaseMapper<CommentEntity> {
    // Phase 1: 仅使用 BaseMapper 提供的方法
    // 后续阶段会添加自定义查询
}
