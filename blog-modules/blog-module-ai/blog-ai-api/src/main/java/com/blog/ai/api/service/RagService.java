package com.blog.ai.api.service;

import com.blog.ai.api.dto.AskRequestDTO;
import com.blog.ai.api.dto.AskResponseDTO;

/**
 * RAG 问答服务接口
 *
 * <p>
 * 基于检索增强生成（RAG）技术，以博客文章内容为知识库，回答用户问题。
 * </p>
 *
 * <p>
 * 流程：用户问题向量化 → 召回相关文章 → 拼接 Prompt → LLM 生成回答
 * </p>
 *
 * @author liusxml
 * @since 1.2.0
 */
public interface RagService {

    /**
     * 基于博客内容回答用户问题
     *
     * @param request 问答请求（包含问题文本）
     * @return 问答响应（包含回答和参考文章来源）
     */
    AskResponseDTO ask(AskRequestDTO request);
}
