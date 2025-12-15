package com.blog.comment.service.impl;

import com.blog.comment.api.dto.CommentDTO;
import com.blog.comment.api.enums.CommentStatus;
import com.blog.comment.api.vo.CommentVO;
import com.blog.comment.domain.entity.CommentEntity;
import com.blog.comment.infrastructure.converter.CommentConverter;
import com.blog.comment.infrastructure.mapper.CommentMapper;
import com.blog.comment.service.ICommentService;
import com.blog.common.base.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 评论服务实现
 *
 * <p>
 * Phase 1 实现要点:
 * </p>
 * <ul>
 * <li>继承 BaseServiceImpl 获得 CRUD 能力</li>
 * <li>使用构造注入（显式调用super）</li>
 * <li>重写 preSave 钩子设置默认值</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.2.0
 */
@Slf4j
@Service
public class CommentServiceImpl
        extends BaseServiceImpl<CommentMapper, CommentEntity, CommentVO, CommentDTO, CommentConverter>
        implements ICommentService {

    /**
     * 构造注入 Converter
     *
     * @param converter MapStruct 转换器
     */
    public CommentServiceImpl(CommentConverter converter) {
        super(converter);
    }

    /**
     * 保存前钩子：设置默认值
     *
     * @param entity 评论实体
     */
    @Override
    protected void preSave(CommentEntity entity) {
        // 1. 设置默认状态（Phase 1 简化，直接通过）
        if (Objects.isNull(entity.getStatus())) {
            entity.setStatus(CommentStatus.APPROVED);
        }

        // 2. 初始化统计字段
        if (Objects.isNull(entity.getLikeCount())) {
            entity.setLikeCount(0);
        }
        if (Objects.isNull(entity.getReplyCount())) {
            entity.setReplyCount(0);
        }

        log.debug("评论保存前处理: status={}, likeCount={}", entity.getStatus(), entity.getLikeCount());
    }
}
