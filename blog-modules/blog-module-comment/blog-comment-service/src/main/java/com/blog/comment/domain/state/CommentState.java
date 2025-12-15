package com.blog.comment.domain.state;

import com.blog.comment.domain.entity.CommentEntity;

/**
 * 评论状态接口
 *
 * <p>
 * 定义评论在不同状态下的行为
 * </p>
 *
 * @author liusxml
 * @since 1.3.0
 */
public interface CommentState {

    /**
     * 审核通过
     *
     * @param comment 评论实体
     * @throws com.blog.common.exception.BusinessException 状态转换不合法时抛出
     */
    void approve(CommentEntity comment);

    /**
     * 审核拒绝
     *
     * @param comment 评论实体
     * @param reason  拒绝原因
     * @throws com.blog.common.exception.BusinessException 状态转换不合法时抛出
     */
    void reject(CommentEntity comment, String reason);

    /**
     * 用户删除
     *
     * @param comment 评论实体
     * @throws com.blog.common.exception.BusinessException 状态转换不合法时抛出
     */
    void deleteByUser(CommentEntity comment);

    /**
     * 管理员删除
     *
     * @param comment 评论实体
     * @param reason  删除原因
     * @throws com.blog.common.exception.BusinessException 状态转换不合法时抛出
     */
    void deleteByAdmin(CommentEntity comment, String reason);

    /**
     * 获取当前状态名称
     */
    String getStateName();
}
