package com.blog.comment.service;

import com.blog.comment.api.dto.CommentDTO;
import com.blog.comment.api.dto.CommentReportDTO;
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

    /**
     * 审核通过评论
     *
     * @param id 评论ID
     */
    void approveComment(Long id);

    /**
     * 审核拒绝评论
     *
     * @param id     评论ID
     * @param reason 拒绝原因
     */
    void rejectComment(Long id, String reason);

    /**
     * 用户删除评论
     *
     * @param id 评论ID
     */
    void deleteCommentByUser(Long id);

    /**
     * 管理员删除评论
     *
     * @param id     评论ID
     * @param reason 删除原因
     */
    void deleteCommentByAdmin(Long id, String reason);

    /**
     * 点赞评论
     *
     * @param commentId 评论ID
     */
    void likeComment(Long commentId);

    /**
     * 取消点赞
     *
     * @param commentId 评论ID
     */
    void unlikeComment(Long commentId);

    /**
     * 检查是否已点赞
     *
     * @param commentId 评论ID
     * @return 是否已点赞
     */
    boolean hasLiked(Long commentId);

    /**
     * 举报评论
     *
     * @param dto 举报DTO
     * @return 举报ID
     */
    Long reportComment(CommentReportDTO dto);

    /**
     * 审核通过举报
     *
     * @param reportId 举报ID
     * @param remark   管理员备注
     */
    void approveReport(Long reportId, String remark);

    /**
     * 审核拒绝举报
     *
     * @param reportId 举报ID
     * @param remark   管理员备注
     */
    void rejectReport(Long reportId, String remark);
}
