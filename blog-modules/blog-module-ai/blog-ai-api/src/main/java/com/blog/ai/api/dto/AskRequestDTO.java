package com.blog.ai.api.dto;

import com.blog.common.base.Identifiable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * RAG 问答请求 DTO
 *
 * @author liusxml
 * @since 1.2.0
 */
@Data
@Schema(description = "RAG 问答请求")
public class AskRequestDTO implements Identifiable<Long>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 请求 ID（可选，用于幂等追踪） */
    @Schema(description = "请求 ID（可选）")
    private Long id;

    /**
     * 用户问题
     */
    @NotBlank(message = "问题不能为空")
    @Size(max = 500, message = "问题长度不能超过 500 字")
    @Schema(description = "用户提问内容", example = "你的博客写了哪些 Java 相关的文章？", requiredMode = Schema.RequiredMode.REQUIRED)
    private String question;
}
