package com.blog.comment.service;

import com.blog.comment.api.dto.CommentDTO;
import com.blog.comment.api.enums.CommentTargetType;
import com.blog.comment.api.vo.CommentTreeVO;
import com.blog.comment.api.vo.CommentVO;
import com.blog.comment.domain.entity.CommentEntity;
import com.blog.common.base.IBaseService;

import java.util.List;

/**
 * 评论服务接口
 *
 * @author liusxml
 * @since 1.2.0
 */
public interface ICommentService extends IBaseService<CommentEntity, CommentVO, CommentDTO> {

    /**
     * 获取评论树（某篇文章下的所有评论）
     *
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @return 评论树列表
     */
    List<CommentTreeVO> getCommentTree(CommentTargetType targetType, Long targetId);

    /**
     * 回复评论
     *
     * @param dto 评论DTO
     * @return 评论ID
     */
    Long replyComment(CommentDTO dto);
}
