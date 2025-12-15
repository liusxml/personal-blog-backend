package com.blog.comment.domain.state;

import com.blog.comment.api.enums.CommentStatus;
import com.blog.comment.domain.entity.CommentEntity;
import org.springframework.stereotype.Component;

/**
 * 已通过状态
 *
 * @author liusxml
 * @since 1.3.0
 */
@Component
public class ApprovedState extends AbstractCommentState {

    @Override
    public void deleteByUser(CommentEntity comment) {
        logStateTransition(comment, comment.getStatus(), CommentStatus.USER_DELETED);
        comment.setStatus(CommentStatus.USER_DELETED);
    }

    @Override
    public void deleteByAdmin(CommentEntity comment, String reason) {
        logStateTransition(comment, comment.getStatus(), CommentStatus.ADMIN_DELETED);
        comment.setStatus(CommentStatus.ADMIN_DELETED);
        comment.setAuditReason(reason); // 保存删除原因
    }

    @Override
    public String getStateName() {
        return "已通过";
    }
}
