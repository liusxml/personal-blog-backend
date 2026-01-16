package com.blog.comment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.comment.api.dto.CommentDTO;
import com.blog.comment.api.dto.CommentQueryDTO;
import com.blog.comment.api.enums.CommentStatus;
import com.blog.comment.api.enums.CommentTargetType;
import com.blog.comment.api.enums.ReportStatus;
import com.blog.comment.api.vo.CommentTreeVO;
import com.blog.comment.api.vo.CommentVO;
import com.blog.comment.domain.entity.CommentEntity;
import com.blog.comment.domain.entity.CommentLikeEntity;
import com.blog.comment.domain.entity.CommentReportEntity;
import com.blog.comment.domain.event.CommentLikedEvent;
import com.blog.comment.domain.event.CommentRepliedEvent;
import com.blog.comment.domain.event.CommentReportedEvent;
import com.blog.comment.domain.event.CommentUnlikedEvent;
import com.blog.comment.domain.event.UserMentionedEvent;
import com.blog.comment.domain.parser.MentionParser;
import com.blog.comment.domain.processor.CommentProcessorChain;
import com.blog.comment.domain.processor.ProcessContext;
import com.blog.comment.domain.state.CommentState;
import com.blog.comment.domain.state.CommentStateFactory;
import com.blog.comment.infrastructure.converter.CommentConverter;
import com.blog.comment.infrastructure.mapper.CommentMapper;
import com.blog.comment.metrics.CommentMetrics;
import com.blog.comment.service.ICommentService;
import com.blog.common.base.BaseServiceImpl;
import com.blog.common.exception.BusinessException;
import com.blog.common.exception.SystemErrorCode;
import com.blog.common.model.PageResult;
import com.blog.common.utils.SecurityUtils;
import com.blog.common.utils.TreeBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论服务实现（Phase 4 扩展 - 责任链模式）
 *
 * @author liusxml
 * @since 1.4.0
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
    private final CommentStateFactory stateFactory;
    private final CommentProcessorChain processorChain;
    private final com.blog.comment.infrastructure.mapper.CommentLikeMapper commentLikeMapper;
    private final com.blog.comment.infrastructure.mapper.CommentReportMapper commentReportMapper;
    private final org.springframework.context.ApplicationEventPublisher applicationEventPublisher;
    private final MentionParser mentionParser;
    private final ObjectMapper objectMapper;
    private final CommentMetrics commentMetrics;

    public CommentServiceImpl(CommentConverter converter,
            CommentStateFactory stateFactory,
            CommentProcessorChain processorChain,
            com.blog.comment.infrastructure.mapper.CommentLikeMapper commentLikeMapper,
            com.blog.comment.infrastructure.mapper.CommentReportMapper commentReportMapper,
            org.springframework.context.ApplicationEventPublisher applicationEventPublisher,
            MentionParser mentionParser,
            ObjectMapper objectMapper,
            CommentMetrics commentMetrics) {
        super(converter);
        this.stateFactory = stateFactory;
        this.processorChain = processorChain;
        this.commentLikeMapper = commentLikeMapper;
        this.commentReportMapper = commentReportMapper;
        this.applicationEventPublisher = applicationEventPublisher;
        this.mentionParser = mentionParser;
        this.objectMapper = objectMapper;
        this.commentMetrics = commentMetrics;
        this.treeBuilder = new TreeBuilder<>(
                CommentTreeVO::getId,
                CommentTreeVO::getParentId,
                CommentTreeVO::setChildren);
    }

    @Override
    protected void preSave(CommentEntity entity) {
        // 设置默认状态为待审核
        if (Objects.isNull(entity.getStatus())) {
            entity.setStatus(CommentStatus.PENDING);
        }

        // 初始化统计字段
        if (Objects.isNull(entity.getLikeCount())) {
            entity.setLikeCount(0);
        }
        if (Objects.isNull(entity.getReplyCount())) {
            entity.setReplyCount(0);
        }

        // ✅ Phase 4: 内容处理链（XSS过滤 → 敏感词过滤 → Markdown渲染）
        ProcessContext context = processorChain.execute(entity.getContent());

        if (!context.isPassed()) {
            throw new BusinessException(SystemErrorCode.PARAM_ERROR,
                    "评论内容不合规: " + context.getFailureReason());
        }

        // 更新处理后的内容和HTML
        entity.setContent(context.getProcessedContent());
        entity.setContentHtml(context.getRenderedHtml());

        // ✅ Phase 6: 解析 @mention
        Set<Long> mentionedUserIds = mentionParser.parseMentions(entity.getContent());
        if (!mentionedUserIds.isEmpty()) {
            try {
                entity.setMentionedUserIds(objectMapper.writeValueAsString(mentionedUserIds));
                log.debug("解析@提及成功: userIds={}", mentionedUserIds);
            } catch (JsonProcessingException e) {
                log.warn("序列化@提及用户ID失败", e);
                entity.setMentionedUserIds(null);
            }
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

        log.debug("评论保存前处理: depth={}, rootId={}, contentHtml={}",
                entity.getDepth(), entity.getRootId(),
                entity.getContentHtml() != null ? "已渲染" : "未渲染");
    }

    /**
     * Phase 4: 更新前处理（处理评论编辑场景）
     */
    @Override
    protected void preUpdate(CommentEntity entity) {
        // ✅ 检测 content 是否变更
        CommentEntity existing = getById(entity.getId());

        if (existing == null) {
            log.warn("评论不存在: id={}", entity.getId());
            return;
        }

        // 只有内容变更时才重新处理
        if (!existing.getContent().equals(entity.getContent())) {
            log.info("评论内容已修改，重新渲染 HTML: id={}", entity.getId());

            // 重新执行责任链
            ProcessContext context = processorChain.execute(entity.getContent());

            if (!context.isPassed()) {
                throw new BusinessException(SystemErrorCode.PARAM_ERROR,
                        "评论内容不合规: " + context.getFailureReason());
            }

            // 更新处理后的内容和HTML
            entity.setContent(context.getProcessedContent());
            entity.setContentHtml(context.getRenderedHtml());

            log.debug("评论内容已重新渲染: contentHtml={}",
                    entity.getContentHtml() != null ? "已渲染" : "未渲染");
        }
    }

    /**
     * 重写保存方法，处理根评论的 path 和 rootId
     * Phase 7 优化：在保存前解析 @mention，避免依赖 getById 查询
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Serializable saveByDto(CommentDTO dto) {
        // ✅ Phase 7: 在保存前解析 @mention（避免缓存问题）
        Set<Long> mentionedUserIds = mentionParser.parseMentions(dto.getContent());
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 调用父类保存
        Long commentId = (Long) super.saveByDto(dto);
        CommentEntity entity = getById(commentId);

        // 如果是根评论（parentId 为 null），更新 path 和 rootId
        if (dto.getParentId() == null) {
            entity.setPath("/" + commentId + "/");
            entity.setRootId(commentId);
            updateById(entity);
            log.debug("根评论后置更新: id={}, path={}, rootId={}", commentId, entity.getPath(), entity.getRootId());
        }

        // ✅ Phase 7: 发布 @mention 事件（使用预先解析的结果）
        if (!mentionedUserIds.isEmpty()) {
            applicationEventPublisher.publishEvent(
                    new UserMentionedEvent(commentId, mentionedUserIds, currentUserId));
            log.info("发布@提及事件: commentId={}, mentionedUserIds={}, mentionerId={}",
                    commentId, mentionedUserIds, currentUserId);
        }

        // ✅ Phase 7: 如果是回复，发布回复事件
        if (dto.getParentId() != null) {
            CommentEntity parent = getById(dto.getParentId());
            if (parent != null) {
                applicationEventPublisher.publishEvent(
                        new CommentRepliedEvent(
                                commentId,
                                dto.getParentId(),
                                parent.getCreateBy(),
                                currentUserId // replierId
                        ));
                log.info("发布回复事件: commentId={}, parentId={}, repliedUserId={}, replierId={}",
                        commentId, dto.getParentId(), parent.getCreateBy(), currentUserId);
            }
        }

        // 记录 Micrometer 指标
        commentMetrics.recordCreate();

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

    // ========== 状态模式 - 审核方法 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveComment(Long id) {
        CommentEntity comment = getById(id);
        if (comment == null) {
            throw new BusinessException(SystemErrorCode.NOT_FOUND, "评论不存在");
        }

        // 获取状态处理器并执行审核通过操作
        CommentState state = stateFactory.getState(comment.getStatus());
        state.approve(comment);

        // 更新数据库
        updateById(comment);

        // 记录 Micrometer 指标
        commentMetrics.recordApprove();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectComment(Long id, String reason) {
        CommentEntity comment = getById(id);
        if (comment == null) {
            throw new BusinessException(SystemErrorCode.NOT_FOUND, "评论不存在");
        }

        CommentState state = stateFactory.getState(comment.getStatus());
        state.reject(comment, reason);

        updateById(comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommentByUser(Long id) {
        CommentEntity comment = getById(id);
        if (comment == null) {
            throw new BusinessException(SystemErrorCode.NOT_FOUND, "评论不存在");
        }

        // TODO: 验证是否为当前用户的评论（Phase 3 后续实现）
        // Long currentUserId = SecurityUtils.getCurrentUserId();
        // if (!comment.getCreateBy().equals(currentUserId)) {
        // throw new BusinessException(SystemErrorCode.ACCESS_DENIED, "无权删除他人评论");
        // }

        CommentState state = stateFactory.getState(comment.getStatus());
        state.deleteByUser(comment);

        updateById(comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommentByAdmin(Long id, String reason) {
        CommentEntity comment = getById(id);
        if (comment == null) {
            throw new BusinessException(SystemErrorCode.NOT_FOUND, "评论不存在");
        }

        CommentState state = stateFactory.getState(comment.getStatus());
        state.deleteByAdmin(comment, reason);

        updateById(comment);
    }

    // ========== Phase 5: 点赞和举报功能 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeComment(Long commentId) {
        Long userId = SecurityUtils.getCurrentUserId();

        // 幂等性检查
        if (commentLikeMapper.hasLiked(userId, commentId)) {
            return; // 已点赞，直接返回
        }

        // 插入点赞记录
        CommentLikeEntity like = new CommentLikeEntity();
        like.setCommentId(commentId);
        like.setUserId(userId);
        commentLikeMapper.insert(like);

        // 发布事件（异步更新计数）
        applicationEventPublisher.publishEvent(new CommentLikedEvent(commentId));

        log.info("用户点赞评论: userId={}, commentId={}", userId, commentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeComment(Long commentId) {
        Long userId = SecurityUtils.getCurrentUserId();

        // 使用逻辑删除（BaseMapper.deleteById 自动使用 @TableLogic）
        CommentLikeEntity like = commentLikeMapper.selectOne(
                new LambdaQueryWrapper<CommentLikeEntity>()
                        .eq(CommentLikeEntity::getUserId, userId)
                        .eq(CommentLikeEntity::getCommentId, commentId));

        if (like != null) {
            commentLikeMapper.deleteById(like.getId());

            // 发布取消点赞事件
            applicationEventPublisher.publishEvent(new CommentUnlikedEvent(commentId));

            log.info("用户取消点赞: userId={}, commentId={}", userId, commentId);
        }
    }

    @Override
    public boolean hasLiked(Long commentId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return commentLikeMapper.hasLiked(userId, commentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reportComment(com.blog.comment.api.dto.CommentReportDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();

        // 创建举报记录
        CommentReportEntity report = new CommentReportEntity();
        report.setCommentId(dto.getCommentId());
        report.setReporterId(userId);
        report.setReasonType(dto.getReasonType());
        report.setReasonDetail(dto.getReasonDetail());
        report.setStatus(ReportStatus.PENDING);

        commentReportMapper.insert(report);

        // 发布举报事件
        applicationEventPublisher.publishEvent(
                new CommentReportedEvent(dto.getCommentId(), report.getId()));

        log.warn("用户举报评论: userId={}, commentId={}, reportId={}, reason={}",
                userId, dto.getCommentId(), report.getId(), dto.getReasonType());

        return report.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReport(Long reportId, String remark) {
        CommentReportEntity report = commentReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(SystemErrorCode.NOT_FOUND, "举报记录不存在");
        }

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new BusinessException(SystemErrorCode.PARAM_ERROR, "该举报已处理，无法重复审核");
        }

        report.setStatus(ReportStatus.APPROVED);
        report.setAdminRemark(remark);
        commentReportMapper.updateById(report);

        log.info("管理员审核通过举报: reportId={}, commentId={}, remark={}",
                reportId, report.getCommentId(), remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectReport(Long reportId, String remark) {
        CommentReportEntity report = commentReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(SystemErrorCode.NOT_FOUND, "举报记录不存在");
        }

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new BusinessException(SystemErrorCode.PARAM_ERROR, "该举报已处理，无法重复审核");
        }

        report.setStatus(ReportStatus.REJECTED);
        report.setAdminRemark(remark);
        commentReportMapper.updateById(report);

        log.info("管理员审核拒绝举报: reportId={}, commentId={}, remark={}",
                reportId, report.getCommentId(), remark);
    }

    // ========== Micrometer 指标查询方法 ==========

    /**
     * 获取评论总数（供 CommentMetrics 使用）
     */
    public long getMetricTotalCount() {
        return baseMapper.selectCount(null);
    }

    /**
     * 获取待审核评论数（供 CommentMetrics 使用）
     */
    public long getMetricPendingCount() {
        return baseMapper.selectCount(
                new LambdaQueryWrapper<CommentEntity>()
                        .eq(CommentEntity::getStatus, CommentStatus.PENDING));
    }

    // ==================== 管理端接口 ====================

    /**
     * 分页查询评论列表（管理端）
     *
     * <p>
     * 与用户端接口的区别：
     * </p>
     * <ul>
     * <li>status=null 时查询所有状态的评论（待审核、已通过、已拒绝、已删除）</li>
     * <li>用于管理后台，需要查看所有评论</li>
     * </ul>
     *
     * @param query 查询参数
     * @return 分页结果
     */
    public PageResult<CommentVO> pageListForAdmin(CommentQueryDTO query) {
        log.info("管理端分页查询评论: pageNum={}, pageSize={}, status={}, targetType={}",
                query.getPageNum(), query.getPageSize(), query.getStatus(), query.getTargetType());

        // 构造分页对象
        Page<CommentEntity> page = new Page<>(query.getPageNum(), query.getPageSize());

        // 构建查询条件（管理端版本）
        LambdaQueryWrapper<CommentEntity> wrapper = buildQueryWrapperForAdmin(query);

        // 使用 BaseServiceImpl 的 pageWithConverter（直接 Entity -> VO）
        IPage<CommentVO> voPage = this.pageWithConverter(page, wrapper, converter::entityToVo);

        // 转换为 PageResult
        return PageResult.of(voPage);
    }

    /**
     * 构建查询条件（管理端版本）
     *
     * <p>
     * 与用户端版本的区别：status=null 时不添加状态过滤，查询所有状态。
     * </p>
     */
    private LambdaQueryWrapper<CommentEntity> buildQueryWrapperForAdmin(CommentQueryDTO query) {
        LambdaQueryWrapper<CommentEntity> wrapper = new LambdaQueryWrapper<>();

        // 基础条件：未删除
        wrapper.eq(CommentEntity::getIsDeleted, 0);

        // 状态筛选（管理端：status=null 时不过滤）
        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            wrapper.eq(CommentEntity::getStatus, CommentStatus.valueOf(query.getStatus()));
        }
        // else: 不添加状态条件，查询所有状态

        // 目标类型筛选
        if (query.getTargetType() != null) {
            wrapper.eq(CommentEntity::getTargetType, query.getTargetType());
        }

        // 目标ID筛选
        if (query.getTargetId() != null) {
            wrapper.eq(CommentEntity::getTargetId, query.getTargetId());
        }

        // 创建者筛选
        if (query.getCreateBy() != null) {
            wrapper.eq(CommentEntity::getCreateBy, query.getCreateBy());
        }

        // 排序：按创建时间倒序
        wrapper.orderByDesc(CommentEntity::getCreateTime);

        return wrapper;
    }
}
