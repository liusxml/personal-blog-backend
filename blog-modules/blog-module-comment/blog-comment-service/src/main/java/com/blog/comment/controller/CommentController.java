package com.blog.comment.controller;

import com.blog.comment.api.dto.CommentDTO;
import com.blog.comment.api.enums.CommentTargetType;
import com.blog.comment.api.vo.CommentTreeVO;
import com.blog.comment.api.vo.CommentVO;
import com.blog.comment.service.ICommentService;
import com.blog.common.exception.BusinessException;
import com.blog.common.exception.SystemErrorCode;
import com.blog.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.util.List;

/**
 * 评论管理 Controller
 *
 * @author liusxml
 * @since 1.2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@Tag(name = "评论管理", description = "评论CRUD接口")
public class CommentController {

    private final ICommentService commentService;

    /**
     * 创建评论
     *
     * @param dto 评论DTO
     * @return 评论ID
     */
    @PostMapping
    @Operation(summary = "创建评论")
    public Result<Long> create(@Valid @RequestBody CommentDTO dto) {
        Serializable id = commentService.saveByDto(dto);
        return Result.success((Long) id);
    }

    /**
     * 回复评论
     *
     * @param dto 评论DTO
     * @return 评论ID
     */
    @PostMapping("/reply")
    @Operation(summary = "回复评论")
    public Result<Long> reply(@Valid @RequestBody CommentDTO dto) {
        return Result.success(commentService.replyComment(dto));
    }

    /**
     * 获取评论详情
     *
     * @param id 评论ID
     * @return 评论VO
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取评论详情")
    public Result<CommentVO> getById(@PathVariable Long id) {
        return commentService.getVoById(id)
                .map(Result::success)
                .orElseThrow(() -> new BusinessException(SystemErrorCode.NOT_FOUND));
    }

    /**
     * 获取评论树
     *
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @return 评论树
     */
    @GetMapping("/tree")
    @Operation(summary = "获取评论树")
    public Result<List<CommentTreeVO>> getTree(
            @RequestParam CommentTargetType targetType,
            @RequestParam Long targetId) {
        return Result.success(commentService.getCommentTree(targetType, targetId));
    }

    /**
     * 更新评论
     *
     * @param dto 评论DTO
     * @return 是否成功
     */
    @PutMapping
    @Operation(summary = "更新评论")
    public Result<Boolean> update(@Valid @RequestBody CommentDTO dto) {
        return Result.success(commentService.updateByDto(dto));
    }

    /**
     * 用户删除自己的评论（状态流转）
     *
     * @param id 评论ID
     * @return 成功提示
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除评论")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteCommentByUser(id);
        return Result.success();
    }

    // ========== Phase 5: 点赞和举报功能 ==========

    /**
     * 点赞评论
     *
     * @param id 评论ID
     * @return 成功提示
     */
    @PostMapping("/{id}/like")
    @Operation(summary = "点赞评论")
    public Result<Void> like(@PathVariable Long id) {
        commentService.likeComment(id);
        return Result.success();
    }

    /**
     * 取消点赞
     *
     * @param id 评论ID
     * @return 成功提示
     */
    @DeleteMapping("/{id}/like")
    @Operation(summary = "取消点赞")
    public Result<Void> unlike(@PathVariable Long id) {
        commentService.unlikeComment(id);
        return Result.success();
    }

    /**
     * 检查是否已点赞
     *
     * @param id 评论ID
     * @return 是否已点赞
     */
    @GetMapping("/{id}/liked")
    @Operation(summary = "检查是否已点赞")
    public Result<Boolean> hasLiked(@PathVariable Long id) {
        return Result.success(commentService.hasLiked(id));
    }

    /**
     * 举报评论
     *
     * @param id  评论ID
     * @param dto 举报DTO
     * @return 举报ID
     */
    @PostMapping("/{id}/report")
    @Operation(summary = "举报评论")
    public Result<Long> report(@PathVariable Long id,
                               @Valid @RequestBody com.blog.comment.api.dto.CommentReportDTO dto) {
        dto.setCommentId(id);
        return Result.success(commentService.reportComment(dto));
    }

    /**
     * 审核通过举报（管理员）
     *
     * @param reportId 举报ID
     * @param remark   管理员备注
     * @return 成功提示
     */
    @PostMapping("/reports/{reportId}/approve")
    @Operation(summary = "审核通过举报", description = "需要管理员权限")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public Result<Void> approveReport(@PathVariable Long reportId,
                                      @org.springframework.web.bind.annotation.RequestParam(required = false) String remark) {
        commentService.approveReport(reportId, remark);
        return Result.success();
    }

    /**
     * 审核拒绝举报（管理员）
     *
     * @param reportId 举报ID
     * @param remark   管理员备注
     * @return 成功提示
     */
    @PostMapping("/reports/{reportId}/reject")
    @Operation(summary = "审核拒绝举报", description = "需要管理员权限")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> rejectReport(@PathVariable Long reportId,
                                     @RequestParam(required = false) String remark) {
        commentService.rejectReport(reportId, remark);
        return Result.success();
    }
}
