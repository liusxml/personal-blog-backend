package com.blog.comment.domain.state;

import com.blog.comment.api.enums.CommentStatus;
import com.blog.comment.domain.entity.CommentEntity;
import com.blog.common.exception.BusinessException;
import com.blog.common.exception.SystemErrorCode;
import lombok.extern.slf4j.Slf4j;

/**
 * 抽象评论状态类
 *
 * <p>
 * 提供通用的状态转换异常处理
 * </p>
 *
 * @author liusxml
 * @since 1.3.0
 */
@Slf4j
public abstract class AbstractCommentState implements CommentState {

    /**
     * 抛出非法状态转换异常
     */
    protected void throwIllegalStateException(String operation) {
        String message = String.format(
                "评论当前状态为 [%s]，无法执行 [%s] 操作",
                getStateName(),
                operation);
        log.warn(message);
        throw new BusinessException(SystemErrorCode.PARAM_ERROR, message);
    }

    /**
     * 记录状态转换日志
     */
    protected void logStateTransition(CommentEntity comment, CommentStatus from, CommentStatus to) {
        log.info("评论状态转换: id={}, {} -> {}", comment.getId(), from.getDescription(), to.getDescription());
    }

    @Override
    public void approve(CommentEntity comment) {
        throwIllegalStateException("审核通过");
    }

    @Override
    public void reject(CommentEntity comment, String reason) {
        throwIllegalStateException("审核拒绝");
    }

    @Override
    public void deleteByUser(CommentEntity comment) {
        throwIllegalStateException("用户删除");
    }

    @Override
    public void deleteByAdmin(CommentEntity comment, String reason) {
        throwIllegalStateException("管理员删除");
    }
}
