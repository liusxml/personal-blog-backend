package com.blog.comment.infrastructure.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.comment.domain.entity.CommentLikeEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论点赞 Mapper
 *
 * @author liusxml
 * @since 1.5.0
 */
@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLikeEntity> {

    /**
     * 查询用户是否已点赞（逻辑删除自动过滤 is_deleted=1）
     *
     * @param userId    用户ID
     * @param commentId 评论ID
     * @return 是否已点赞
     */
    default boolean hasLiked(Long userId, Long commentId) {
        return exists(new LambdaQueryWrapper<CommentLikeEntity>()
                .eq(CommentLikeEntity::getUserId, userId)
                .eq(CommentLikeEntity::getCommentId, commentId));
    }
}
