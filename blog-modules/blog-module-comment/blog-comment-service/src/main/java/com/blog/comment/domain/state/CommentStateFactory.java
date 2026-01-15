package com.blog.comment.domain.state;

import com.blog.comment.api.enums.CommentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 评论状态工厂
 *
 * <p>
 * 根据评论状态返回对应的状态处理器
 * </p>
 *
 * @author liusxml
 * @since 1.3.0
 */
@Component
@RequiredArgsConstructor
public class CommentStateFactory {

    private final PendingState pendingState;
    private final ApprovedState approvedState;
    private final RejectedState rejectedState;
    private final DeletedState deletedState;

    /**
     * 状态映射表
     */
    private Map<CommentStatus, CommentState> getStateMap() {
        return Map.of(
                CommentStatus.PENDING, pendingState,
                CommentStatus.APPROVED, approvedState,
                CommentStatus.REJECTED, rejectedState,
                CommentStatus.USER_DELETED, deletedState,
                CommentStatus.ADMIN_DELETED, deletedState);
    }

    /**
     * 根据状态获取对应的处理器
     *
     * @param status 评论状态
     * @return 状态处理器
     */
    public CommentState getState(CommentStatus status) {
        return getStateMap().get(status);
    }
}
