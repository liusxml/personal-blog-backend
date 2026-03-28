package com.blog.ai.controller;

import com.blog.ai.api.dto.AskRequestDTO;
import com.blog.ai.api.dto.AskResponseDTO;
import com.blog.ai.api.service.RagService;
import com.blog.ai.api.service.TextEmbeddingService;
import com.blog.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * AI 服务对外接口
 *
 * <p>
 * 公开接口，无需登录（已在 SecurityConfig 中配置 /api/v1/ai/** permitAll）。
 * </p>
 *
 * @author liusxml
 * @since 1.2.0
 */
@Tag(name = "AI", description = "AI 问答与健康检查")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final RagService ragService;
    private final TextEmbeddingService embeddingService;

    /**
     * RAG 博客内容问答
     *
     * <p>基于博客文章向量库，回答用户关于博客内容的问题。</p>
     */
    @Operation(summary = "RAG 问答", description = "基于博客文章内容语义检索，由 LLM 生成回答")
    @PostMapping("/ask")
    public Result<AskResponseDTO> ask(@RequestBody @Valid AskRequestDTO request) {
        return Result.success(ragService.ask(request));
    }

    /**
     * AI 服务健康检查
     *
     * <p>检查 Embedding 服务是否可用（API Key 已配置、网络可达）。</p>
     */
    @Operation(summary = "AI 健康检查")
    @GetMapping("/health")
    public Result<String> health() {
        String status = embeddingService.isAvailable() ? "UP" : "DOWN";
        return Result.success(status);
    }
}
