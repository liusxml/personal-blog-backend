package com.blog.comment.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.comment.domain.entity.CommentReportEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论举报 Mapper
 *
 * @author liusxml
 * @since 1.5.0
 */
@Mapper
public interface CommentReportMapper extends BaseMapper<CommentReportEntity> {
}
