package com.blog.ai.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * RAG 问答响应 VO
 *
 * @author liusxml
 * @since 1.2.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RAG 问答响应")
public class AskResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** LLM 生成的回答 */
    @Schema(description = "AI 生成的回答内容")
    private String answer;

    /**
     * 本次回答参考的文章列表
     */
    @Schema(description = "参考来源文章列表")
    private List<ArticleSummaryDTO> sources;
}
