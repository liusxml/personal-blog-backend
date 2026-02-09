package com.blog.article.controller;

import com.blog.article.service.BingWallpaperService;
import com.blog.common.exception.SystemErrorCode;
import com.blog.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 壁纸管理控制器（管理端）
 *
 * <p>
 * 提供壁纸相关的管理功能，用于Admin端选择封面图。
 * </p>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/wallpapers")
@RequiredArgsConstructor
@Tag(name = "壁纸管理（管理端）", description = "壁纸相关的管理功能")
public class WallpaperAdminController {

    private final BingWallpaperService bingWallpaperService;

    /**
     * 获取随机必应壁纸
     *
     * <p>
     * 从近7天的必应壁纸中随机返回一张，用于Admin端文章封面选择。
     * </p>
     *
     * <p>
     * 此接口作为代理，解决前端直接调用必应API的CORS问题。
     * </p>
     *
     * @return 壁纸URL
     */
    @GetMapping("/bing")
    @Operation(summary = "获取随机必应壁纸", description = "返回一张随机必应壁纸URL")
    public Result<String> getBingWallpaper() {
        log.info("请求获取随机必应壁纸");

        String wallpaperUrl = bingWallpaperService.getRandomWallpaper();

        if (org.apache.commons.lang3.StringUtils.isBlank(wallpaperUrl)) {
            log.error("获取必应壁纸失败：返回URL为空");
            return Result.error(SystemErrorCode.SYSTEM_ERROR, "获取壁纸失败");
        }

        log.debug("成功获取必应壁纸: {}", wallpaperUrl);
        return Result.success(wallpaperUrl);
    }
}
