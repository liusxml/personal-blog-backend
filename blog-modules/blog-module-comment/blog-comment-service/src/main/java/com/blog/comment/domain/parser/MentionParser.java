package com.blog.comment.domain.parser;

import com.blog.system.api.service.IUserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @提及解析器
 *        解析评论中的 @username，返回用户ID列表
 *
 * @author liusxml
 * @since 1.6.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MentionParser {

    /**
     * @提及正则：@后跟3-20位字母数字下划线
     */
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\w]{3,20})(?![\\w@])");

    private final IUserQueryService userQueryService;

    /**
     * 解析评论中的 @username
     *
     * @param content 评论内容
     * @return 提及的用户ID集合
     */
    public Set<Long> parseMentions(String content) {
        if (StringUtils.isBlank(content)) {
            return Collections.emptySet();
        }

        Set<String> usernames = new HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);

        while (matcher.find()) {
            usernames.add(matcher.group(1));
        }

        if (usernames.isEmpty()) {
            return Collections.emptySet();
        }

        // 批量查询用户ID（通过接口调用）
        List<Long> userIds = userQueryService.getUserIdsByUsernames(usernames);
        log.debug("解析@提及: usernames={}, userIds={}", usernames, userIds);

        return new HashSet<>(userIds);
    }

    /**
     * 高亮 @username（HTML）
     *
     * @param html HTML内容
     * @return 高亮后的HTML
     */
    public String highlightMentions(String html) {
        if (StringUtils.isBlank(html)) {
            return html;
        }
        return html.replaceAll("@([\\w]{3,20})", "<span class=\"mention\">@$1</span>");
    }
}
