package com.blog.common.event;

/**
 * 文件访问预签名 URL 刷新完成事件
 *
 * <p>
 * 当文件模块重新签发新的 S3 预签名访问 URL 时发布此事件，
 * 供文章等业务模块监听并同步回写数据库中的封面 URL，消除过期 URL 残留。
 * </p>
 *
 * @param fileId    文件主键 ID
 * @param accessUrl 新生成的预签名访问 URL
 * @author liusxml
 * @since 1.1.0
 */
public record FileAccessUrlRefreshedEvent(Long fileId, String accessUrl) {
}
