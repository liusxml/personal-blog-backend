package com.blog.comment.domain.state;

import org.springframework.stereotype.Component;

/**
 * 已删除状态（终态，不允许任何转换）
 *
 * @author liusxml
 * @since 1.3.0
 */
@Component
public class DeletedState extends AbstractCommentState {

    @Override
    public String getStateName() {
        return "已删除";
    }
}
