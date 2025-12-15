package com.blog.comment.api.controller;

import com.blog.comment.api.dto.CommentDTO;
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
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;

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
     * 删除评论
     *
     * @param id 评论ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除评论")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(commentService.removeById(id));
    }
}
