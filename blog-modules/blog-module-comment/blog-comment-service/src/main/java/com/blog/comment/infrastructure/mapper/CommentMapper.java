package com.blog.comment.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.comment.domain.entity.CommentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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

    /**
     * 增加点赞数 +1
     *
     * @param commentId 评论ID
     */
    @Update("UPDATE cmt_comment SET like_count = like_count + 1 WHERE id = #{commentId}")
    void incrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 减少点赞数 -1（防止负数）
     *
     * @param commentId 评论ID
     */
    @Update("UPDATE cmt_comment SET like_count = like_count - 1 WHERE id = #{commentId} AND like_count > 0")
    void decrementLikeCount(@Param("commentId") Long commentId);
}
