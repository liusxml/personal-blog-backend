package com.blog.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S3 预签名 URL 工具类
 *
 * <p>
 * 解析 AWS SigV4 预签名参数（{@code X-Amz-Date} 与 {@code X-Amz-Expires}），
 * 在内存中零 I/O 判定链接是否已过期或即将过期。
 * </p>
 *
 * @author liusxml
 * @since 1.1.0
 */
public final class PresignedUrlUtils {

    /**
     * 提前刷新安全缓冲窗口（秒）：距离真实过期不足 1 小时即视为需刷新
     */
    private static final long REFRESH_BUFFER_SECONDS = 3600L;

    private static final Pattern AMZ_DATE_PATTERN = Pattern.compile("[?&]X-Amz-Date=(\\d{8}T\\d{6}Z)");
    private static final Pattern AMZ_EXPIRES_PATTERN = Pattern.compile("[?&]X-Amz-Expires=(\\d+)");

    private static final DateTimeFormatter AMZ_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);

    private PresignedUrlUtils() {
    }

    /**
     * 判断 S3 SigV4 预签名 URL 是否已过期（或距离过期不足 1 小时）
     *
     * <p>
     * 非 S3 预签名链接（如必应壁纸、本地静态路径）统一返回 {@code false}。
     * </p>
     *
     * @param url 待检测的图片/文件 URL
     * @return {@code true} 表示为 S3 预签名链接且已过期（或即将过期），需要刷新
     */
    public static boolean isPresignedUrlExpired(String url) {
        if (StringUtils.isBlank(url) || !url.contains("X-Amz-Date=") || !url.contains("X-Amz-Expires=")) {
            return false;
        }

        Matcher dateMatcher = AMZ_DATE_PATTERN.matcher(url);
        Matcher expiresMatcher = AMZ_EXPIRES_PATTERN.matcher(url);
        if (!dateMatcher.find() || !expiresMatcher.find()) {
            return false;
        }

        try {
            Instant signedAt = AMZ_DATE_FORMATTER.parse(dateMatcher.group(1), Instant::from);
            long expireSeconds = Long.parseLong(expiresMatcher.group(1));
            Instant refreshThreshold = signedAt.plusSeconds(Math.max(0L, expireSeconds - REFRESH_BUFFER_SECONDS));
            return !Instant.now().isBefore(refreshThreshold);
        } catch (Exception e) {
            return false;
        }
    }
}
