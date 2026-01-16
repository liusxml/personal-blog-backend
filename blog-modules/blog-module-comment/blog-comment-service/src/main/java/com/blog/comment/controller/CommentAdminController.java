package com.blog.comment.controller;

import com.blog.comment.api.dto.CommentQueryDTO;
import com.blog.comment.api.vo.CommentVO;
import com.blog.comment.service.ICommentService;
import com.blog.common.model.PageResult;
import com.blog.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论管理 Controller（管理端）
 *
 * @author liusxml
 * @since 1.4.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/comments")
@RequiredArgsConstructor
@Tag(name = "评论管理（管理端）", description = "管理员评论查询接口")
public class CommentAdminController {

    private final ICommentService commentService;

    /**
     * 获取评论列表（分页）
     *
     * <p>
     * 管理端可以查询所有状态的评论（待审核、已通过、已拒绝、已删除）
     * </p>
     *
     * @param query 查询参数
     * @return 评论列表
     */
    @GetMapping
    @Operation(summary = "获取评论列表", description = "分页查询所有评论")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<CommentVO>> listComments(@Valid CommentQueryDTO query) {
        log.info("管理端查询评论列表: targetType={}, status={}, page={}/{}",
                query.getTargetType(), query.getStatus(),
                query.getPageNum(), query.getPageSize());

        // 调用管理端专用的查询方法
        PageResult<CommentVO> pageResult = commentService.pageListForAdmin(query);

        return Result.success(pageResult);
    }
}
