package com.blog.article.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 必应壁纸服务
 *
 * <p>从必应近7天壁纸中随机选择，作为文章默认封面图</p>
 * <p>随机策略避免同一天创建的多篇文章使用相同壁纸</p>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BingWallpaperService {

    private static final String BING_API = "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=7&mkt=zh-CN";

    private final RestTemplate restTemplate;

    /**
     * 从近7天必应壁纸中随机选择一张
     *
     * <p>通过随机策略降低重复概率（约14%）</p>
     *
     * @return 壁纸URL，失败返回null
     */
    public String getRandomWallpaper() {
        try {
            Map<String, Object> response = restTemplate.getForObject(BING_API, Map.class);

            if (response != null && response.containsKey("images")) {
                List<?> images = (List<?>) response.get("images");
                if (!images.isEmpty()) {
                    // 随机选择一张（Java 21 ThreadLocalRandom）
                    int randomIndex = ThreadLocalRandom.current().nextInt(images.size());
                    Map<String, Object> image = (Map<String, Object>) images.get(randomIndex);
                    String url = (String) image.get("url");

                    log.debug("随机选择第{}张必应壁纸（共{}张）", randomIndex + 1, images.size());
                    return "https://www.bing.com" + url;
                }
            }
        } catch (Exception e) {
            log.error("获取必应壁纸失败", e);
        }

        return null;
    }
}
