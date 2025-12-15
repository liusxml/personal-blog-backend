package com.blog.comment.controller;

import com.blog.comment.api.dto.CommentAuditDTO;
import com.blog.comment.service.ICommentService;
import com.blog.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 评论审核 Controller（管理员专用）
 *
 * @author liusxml
 * @since 1.3.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/comments/audit")
@RequiredArgsConstructor
@Tag(name = "评论审核管理", description = "管理员审核评论接口")
public class CommentAuditController {

    private final ICommentService commentService;

    /**
     * 审核通过评论
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "审核通过评论")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> approve(@PathVariable Long id) {
        commentService.approveComment(id);
        return Result.success();
    }

    /**
     * 审核拒绝评论
     */
    @PostMapping("/{id}/reject")
    @Operation(summary = "审核拒绝评论")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> reject(
            @PathVariable Long id,
            @Valid @RequestBody CommentAuditDTO dto) {
        commentService.rejectComment(id, dto.getReason());
        return Result.success();
    }

    /**
     * 管理员删除评论
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "管理员删除评论")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteByAdmin(
            @PathVariable Long id,
            @Valid @RequestBody CommentAuditDTO dto) {
        commentService.deleteCommentByAdmin(id, dto.getReason());
        return Result.success();
    }
}
