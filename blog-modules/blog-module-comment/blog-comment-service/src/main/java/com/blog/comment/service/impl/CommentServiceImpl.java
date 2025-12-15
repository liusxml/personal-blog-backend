package com.blog.comment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.comment.api.dto.CommentDTO;
import com.blog.comment.api.enums.CommentStatus;
import com.blog.comment.api.enums.CommentTargetType;
import com.blog.comment.api.vo.CommentTreeVO;
import com.blog.comment.api.vo.CommentVO;
import com.blog.comment.domain.entity.CommentEntity;
import com.blog.comment.infrastructure.converter.CommentConverter;
import com.blog.comment.infrastructure.mapper.CommentMapper;
import com.blog.comment.service.ICommentService;
import com.blog.common.base.BaseServiceImpl;
import com.blog.common.exception.BusinessException;
import com.blog.common.exception.SystemErrorCode;
import com.blog.common.util.TreeBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 评论服务实现（Phase 2 扩展）
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
     * 最大嵌套深度
     */
    private static final int MAX_DEPTH = 5;

    private final TreeBuilder<CommentTreeVO, Long> treeBuilder;

    public CommentServiceImpl(CommentConverter converter) {
        super(converter);
        this.treeBuilder = new TreeBuilder<>(
                CommentTreeVO::getId,
                CommentTreeVO::getParentId,
                CommentTreeVO::setChildren);
    }

    @Override
    protected void preSave(CommentEntity entity) {
        // 设置默认状态
        if (Objects.isNull(entity.getStatus())) {
            entity.setStatus(CommentStatus.APPROVED);
        }

        // 初始化统计字段
        if (Objects.isNull(entity.getLikeCount())) {
            entity.setLikeCount(0);
        }
        if (Objects.isNull(entity.getReplyCount())) {
            entity.setReplyCount(0);
        }

        // 计算树形字段
        if (Objects.isNull(entity.getParentId())) {
            // 根评论
            entity.setDepth(0);
            entity.setRootId(null); // 待保存后更新为自身ID
        } else {
            // 回复评论，从父评论继承路径信息
            CommentEntity parent = getById(entity.getParentId());
            if (parent == null) {
                throw new BusinessException(SystemErrorCode.PARAM_ERROR,
                        "父评论不存在，parentId=" + entity.getParentId() + "。请先创建根评论再进行回复。");
            }

            // 深度限制
            if (parent.getDepth() >= MAX_DEPTH) {
                throw new BusinessException(SystemErrorCode.PARAM_ERROR,
                        "评论嵌套深度超过限制（最大" + MAX_DEPTH + "层），当前深度=" + parent.getDepth());
            }

            entity.setDepth(parent.getDepth() + 1);
            entity.setRootId(parent.getRootId() != null ? parent.getRootId() : parent.getId());
        }

        log.debug("评论保存前处理: depth={}, rootId={}", entity.getDepth(), entity.getRootId());
    }

    /**
     * 重写保存方法，处理根评论的 path 和 rootId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Serializable saveByDto(CommentDTO dto) {
        // 调用父类保存
        Long commentId = (Long) super.saveByDto(dto);

        // 如果是根评论（parentId 为 null），更新 path 和 rootId
        if (dto.getParentId() == null) {
            CommentEntity entity = getById(commentId);
            entity.setPath("/" + commentId + "/");
            entity.setRootId(commentId);
            updateById(entity);
            log.debug("根评论后置更新: id={}, path={}, rootId={}", commentId, entity.getPath(), entity.getRootId());
        }

        return commentId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long replyComment(CommentDTO dto) {
        // 校验父评论存在
        if (dto.getParentId() == null) {
            throw new BusinessException(SystemErrorCode.PARAM_ERROR,
                    "回复评论时必须指定父评论ID。如需创建根评论，请使用 POST /api/v1/comments 接口。");
        }

        // 保存评论（preSave会处理树形字段）
        Long commentId = (Long) saveByDto(dto);

        // 更新path字段（需要用到ID）
        CommentEntity entity = getById(commentId);
        CommentEntity parent = getById(dto.getParentId());

        String newPath = (parent.getPath() != null ? parent.getPath() : "/" + parent.getId() + "/")
                + commentId + "/";
        entity.setPath(newPath);

        // 如果是根评论，rootId设为自身
        if (entity.getRootId() == null) {
            entity.setRootId(commentId);
        }

        updateById(entity);

        // 更新父评论的reply_count
        parent.setReplyCount(parent.getReplyCount() + 1);
        updateById(parent);

        return commentId;
    }

    @Override
    public List<CommentTreeVO> getCommentTree(CommentTargetType targetType, Long targetId) {
        // 查询所有评论（扁平列表）
        List<CommentEntity> entities = list(new LambdaQueryWrapper<CommentEntity>()
                .eq(CommentEntity::getTargetType, targetType)
                .eq(CommentEntity::getTargetId, targetId)
                .eq(CommentEntity::getStatus, CommentStatus.APPROVED)
                .orderByAsc(CommentEntity::getCreateTime));

        // 转换为 TreeVO
        List<CommentTreeVO> flatList = entities.stream()
                .map(this::entityToTreeVO)
                .collect(Collectors.toList());

        // 组装树形结构
        return treeBuilder.buildForest(flatList);
    }

    /**
     * Entity 转 TreeVO
     */
    private CommentTreeVO entityToTreeVO(CommentEntity entity) {
        CommentTreeVO vo = new CommentTreeVO();
        vo.setId(entity.getId());
        vo.setTargetType(entity.getTargetType());
        vo.setTargetId(entity.getTargetId());
        vo.setParentId(entity.getParentId());
        vo.setContent(entity.getContent());
        vo.setStatus(entity.getStatus());
        vo.setLikeCount(entity.getLikeCount());
        vo.setReplyCount(entity.getReplyCount());
        vo.setCreateBy(entity.getCreateBy());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setPath(entity.getPath());
        vo.setDepth(entity.getDepth());
        vo.setRootId(entity.getRootId());
        return vo;
    }
}
