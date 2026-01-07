package com.blog.file.controller;

import com.blog.common.model.Result;
import com.blog.file.api.dto.PreSignedUrlRequest;
import com.blog.file.api.vo.FileVO;
import com.blog.file.api.vo.PreSignedUploadVO;
import com.blog.file.service.impl.FileServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * 文件控制器
 *
 * <p>
 * 遵循项目规范：
 * <ul>
 * <li>返回 Result&lt;T&gt; 统一响应</li>
 * <li>使用 @Tag 和 @Operation 注解（OpenAPI文档）</li>
 * <li>使用 @Valid 进行参数校验</li>
 * <li>RESTful API 设计</li>
 * </ul>
 * </p>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "文件上传、查询、删除等功能")
public class FileController {

    private final FileServiceImpl fileService;

    /**
     * 生成预签名上传 URL
     *
     * <p>
     * 前端流程：
     * <ol>
     * <li>调用此接口获取预签名 URL</li>
     * <li>使用 PUT 方法直传文件到 Bitiful</li>
     * <li>上传成功后调用 confirmUpload 接口</li>
     * </ol>
     * </p>
     */
    @PostMapping("/presigned")
    @Operation(summary = "生成预签名上传URL", description = "返回预签名URL供前端直传Bitiful S4")
    public Result<PreSignedUploadVO> generateUploadUrl(@Valid @RequestBody PreSignedUrlRequest request) {
        log.info("生成预签名URL请求: fileName={}", request.getFileName());

        PreSignedUploadVO vo = fileService.generateUploadUrl(request);

        return Result.success(vo);
    }

    /**
     * 确认上传完成
     *
     * <p>
     * 前端上传成功后调用此接口，更新文件状态。
     * </p>
     */
    @PatchMapping("/{fileId}/confirm")
    @Operation(summary = "确认上传完成", description = "前端上传成功后调用，更新文件状态为已完成")
    public Result<Void> confirmUpload(@PathVariable Long fileId) {
        log.info("确认上传: fileId={}", fileId);

        fileService.confirmUpload(fileId);

        return Result.success();
    }

    /**
     * 根据 ID 获取文件信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取文件信息", description = "根据ID查询文件详细信息")
    public Result<FileVO> getById(@PathVariable Long id) {
        log.info("查询文件: id={}", id);

        Optional<FileVO> vo = fileService.getVoById(id);

        return vo.map(Result::success)
                .orElseGet(() -> Result.error(com.blog.common.exception.SystemErrorCode.NOT_FOUND));
    }

    /**
     * 删除文件
     *
     * <p>
     * 软删除 + 调用存储删除。
     * </p>
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除文件", description = "逻辑删除文件记录并删除存储中的文件")
    public Result<Void> deleteById(@PathVariable Long id) {
        log.info("删除文件: id={}", id);

        boolean success = fileService.deleteById(id);

        return success ? Result.success() : Result.error(com.blog.common.exception.SystemErrorCode.SYSTEM_ERROR);
    }

    /**
     * 获取文件访问 URL（动态生成）
     *
     * <p>
     * 不再存储 accessUrl，而是动态生成7天有效的预签名 GET URL。
     * </p>
     */
    @GetMapping("/{id}/access-url")
    @Operation(summary = "获取文件访问URL", description = "动态生成预签名GET URL（7天有效）")
    public Result<String> getAccessUrl(@PathVariable Long id,
                                       @RequestParam(defaultValue = "60") int expireMinutes) {
        log.info("获取访问URL: id={}, expireMinutes={}", id, expireMinutes);

        String accessUrl = fileService.getAccessUrl(id, expireMinutes);

        return Result.success(accessUrl);
    }
}
