package com.blog.common.utils;

import com.blog.common.security.JwtAuthenticationDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security 工具类
 * <p>
 * 提供从 Security Context 获取当前登录用户信息的便捷方法。
 *
 * @author liusxml
 * @since 1.0.0
 */
public class SecurityUtils {

    private SecurityUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 获取当前登录用户的用户名
     *
     * @return 用户名，如果未登录则返回 null
     */
    public static String getCurrentUsername() {
        Authentication authentication = getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userDetails.getUsername();
        }
        return null;
    }

    /**
     * 获取当前登录用户的用户ID
     *
     * @return 用户ID，如果未登录则返回 null
     */
    public static Long getCurrentUserId() {
        Authentication authentication = getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof JwtAuthenticationDetails) {
            JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();
            return details.getUserId();
        }
        return null;
    }

    /**
     * 获取当前登录用户的角色列表
     *
     * @return 角色列表（如 ["ROLE_ADMIN", "ROLE_USER"]）
     */
    public static List<String> getCurrentUserRoles() {
        Authentication authentication = getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * 判断当前用户是否拥有指定角色
     *
     * @param role 角色名（如 "ROLE_ADMIN"）
     * @return true 如果拥有该角色
     */
    public static boolean hasRole(String role) {
        return getCurrentUserRoles().contains(role);
    }

    /**
     * 获取当前认证对象
     */
    private static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
