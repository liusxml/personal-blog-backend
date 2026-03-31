package com.blog.ai.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 相关文章摘要 VO
 *
 * <p>RAG 问答接口返回时，携带召回的相关文章摘要信息，方便前端展示来源。</p>
 *
 * @author liusxml
 * @since 1.2.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "相关文章摘要")
public class ArticleSummaryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 文章 ID */
    @Schema(description = "文章 ID")
    private Long id;

    /** 文章标题 */
    @Schema(description = "文章标题")
    private String title;

    /** 文章访问 URL */
    @Schema(description = "文章链接")
    private String url;

    /** 向量相似度分数（0~1，越高越相关） */
    @Schema(description = "相似度分数", example = "0.92")
    private Double score;
}
