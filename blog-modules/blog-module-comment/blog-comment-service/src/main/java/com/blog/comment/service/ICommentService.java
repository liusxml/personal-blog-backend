package com.blog.comment.service;

import com.blog.comment.api.dto.CommentDTO;
import com.blog.comment.api.vo.CommentVO;
import com.blog.comment.domain.entity.CommentEntity;
import com.blog.common.base.IBaseService;

/**
 * 评论服务接口
 *
 * @author liusxml
 * @since 1.2.0
 */
public interface ICommentService extends IBaseService<CommentEntity, CommentVO, CommentDTO> {
    // Phase 1: 仅继承基础CRUD方法
    // 后续阶段会添加业务方法:
    // - List<CommentTreeVO> buildCommentTree(Long targetId);
    // - void approveComment(Long commentId);
    // - void likeComment(Long commentId);
}
