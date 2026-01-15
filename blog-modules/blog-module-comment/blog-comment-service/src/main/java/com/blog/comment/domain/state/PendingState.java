package com.blog.comment.domain.state;

import com.blog.comment.api.enums.CommentStatus;
import com.blog.comment.domain.entity.CommentEntity;
import org.springframework.stereotype.Component;

/**
 * 待审核状态
 *
 * @author liusxml
 * @since 1.3.0
 */
@Component
public class PendingState extends AbstractCommentState {

    @Override
    public void approve(CommentEntity comment) {
        logStateTransition(comment, comment.getStatus(), CommentStatus.APPROVED);
        comment.setStatus(CommentStatus.APPROVED);
        // TODO: 发布评论通过事件（Phase 3 后续实现）
    }

    @Override
    public void reject(CommentEntity comment, String reason) {
        logStateTransition(comment, comment.getStatus(), CommentStatus.REJECTED);
        comment.setStatus(CommentStatus.REJECTED);
        comment.setAuditReason(reason); // 保存拒绝原因
    }

    @Override
    public void deleteByAdmin(CommentEntity comment, String reason) {
        logStateTransition(comment, comment.getStatus(), CommentStatus.ADMIN_DELETED);
        comment.setStatus(CommentStatus.ADMIN_DELETED);
        comment.setAuditReason(reason); // 保存删除原因
    }

    @Override
    public String getStateName() {
        return "待审核";
    }
}
