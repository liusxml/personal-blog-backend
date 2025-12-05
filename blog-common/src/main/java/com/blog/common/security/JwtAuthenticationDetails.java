package com.blog.common.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * JWT 认证详情，用于存储用户ID等额外信息
 *
 * @author liusxml
 * @since 1.0.0
 */
@Getter
public class JwtAuthenticationDetails extends WebAuthenticationDetails {

    private final Long userId;

    public JwtAuthenticationDetails(HttpServletRequest request, Long userId) {
        super(request);
        this.userId = userId;
    }
}
