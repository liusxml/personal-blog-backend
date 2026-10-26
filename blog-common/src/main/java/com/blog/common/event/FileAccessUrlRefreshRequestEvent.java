package com.blog.common.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * 文件访问预签名 URL 按需刷新请求事件
 *
 * <p>
 * 当文章查询检测到数据库中保存的 S3 预签名 URL 已过期（或即将过期）时，
 * 通过 Spring 事件总线同步请求文件模块重新签发 7 天有效期的预签名 URL。
 * </p>
 *
 * @author liusxml
 * @since 1.1.0
 */
@Getter
@RequiredArgsConstructor
public class FileAccessUrlRefreshRequestEvent {

    /**
     * 文件主键 ID
     */
    private final Long fileId;

    /**
     * 预签名有效期（分钟）
     */
    private final int expireMinutes;

    /**
     * 刷新后的访问 URL（由文件服务监听器回填）
     */
    @Setter
    private String refreshedAccessUrl;
}
