package com.blog.comment.domain.processor;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 评论敏感词过滤处理器
 *
 * <p>
 * 使用简化版 DFA 算法过滤敏感词
 * </p>
 *
 * @author liusxml
 * @since 1.4.0
 */
@Slf4j
@Component
public class CommentSensitiveWordProcessor implements ContentProcessor {

    // TODO: Phase 4 后续可从数据库加载
    private static final Set<String> SENSITIVE_WORDS = Sets.newHashSet(
            "敏感词1", "敏感词2", "违禁词");

    private static final String REPLACEMENT = "***";

    @Override
    public ProcessResult process(ProcessContext context) {
        String content = context.getProcessedContent();

        if (StringUtils.isBlank(content)) {
            return ProcessResult.success();
        }

        String filtered = filterSensitiveWords(content);

        if (!filtered.equals(content)) {
            log.warn("检测到敏感词，已替换");
            context.updateContent(filtered);
            context.getMetadata().put("sensitive_word_filtered", true);
        }

        return ProcessResult.success();
    }

    /**
     * 过滤敏感词
     */
    private String filterSensitiveWords(String content) {
        String result = content;

        for (String word : SENSITIVE_WORDS) {
            if (result.contains(word)) {
                result = result.replace(word, REPLACEMENT);
            }
        }

        return result;
    }

    @Override
    public String getName() {
        return "敏感词过滤器";
    }

    @Override
    public int getOrder() {
        return 200; // 第二执行
    }
}
