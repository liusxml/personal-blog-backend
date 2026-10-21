package com.blog.article.metrics;

import com.blog.article.service.impl.ArticleServiceImpl;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 文章模块 Micrometer 指标采集组件
 *
 * <p>
 * 暴露以下业务指标供 Actuator 使用：
 * </p>
 * <ul>
 * <li><b>blog.articles.total.count</b> - 文章总数（Gauge，实时）</li>
 * <li><b>blog.articles.published.count</b> - 已发布文章数（Gauge，实时）</li>
 * <li><b>blog.articles.published</b> - 累计发布次数（Counter，累计）</li>
 * <li><b>blog.articles.views</b> - 累计浏览量（Counter，累计）</li>
 * </ul>
 *
 * <p>
 * 性能优化：使用缓存机制，避免 Gauge 每次查询数据库
 * </p>
 *
 * @author liusxml
 * @since 1.3.0
 */
@Slf4j
@Component
public class ArticleMetrics {

    private final MeterRegistry registry;

    @Lazy // ✅ 延迟注入，打破循环依赖
    private final ArticleServiceImpl articleService;

    // Gauge 缓存值
    private final AtomicLong totalCountCache = new AtomicLong(0);
    private final AtomicLong publishedCountCache = new AtomicLong(0);

    // Counter 实例
    private Counter publishCounter;
    private Counter viewCounter;

    /**
     * 构造函数（支持 @Lazy 注入）
     */
    public ArticleMetrics(MeterRegistry registry, @Lazy ArticleServiceImpl articleService) {
        this.registry = registry;
        this.articleService = articleService;
    }

    /**
     * 初始化指标注册（仅向 MeterRegistry 注册指标定义，不触发数据库查询）
     */
    @PostConstruct
    public void init() {
        // 1. 注册文章总数 Gauge（读取缓存值）
        registry.gauge("blog.articles.total.count",
                totalCountCache,
                AtomicLong::get);

        // 2. 注册已发布文章数 Gauge（读取缓存值）
        registry.gauge("blog.articles.published.count",
                publishedCountCache,
                AtomicLong::get);

        // 3. 注册累计发布次数 Counter
        publishCounter = Counter.builder("blog.articles.published")
                .description("累计发布文章次数")
                .tag("operation", "publish")
                .register(registry);

        // 4. 注册累计浏览量 Counter
        viewCounter = Counter.builder("blog.articles.views")
                .description("累计文章浏览量")
                .tag("module", "article")
                .register(registry);

        log.info("✅ 文章模块 Micrometer 指标已启用");
    }

    /**
     * 刷新缓存（在应用完全就绪后自动执行首次预热，也可由定时任务或手动调用）
     *
     * <p><b>为什么使用 {@code @EventListener(ApplicationReadyEvent.class)} 而不是在 {@code @PostConstruct} 中调用？</b><br>
     * 因为 {@link ArticleServiceImpl} 构造器强依赖 {@link ArticleMetrics}。如果在 {@code @PostConstruct} 阶段
     * 立即通过 {@code @Lazy} 代理调用 {@code articleService}，此时外层的 {@code ArticleServiceImpl} 尚未完成构造，
     * 会触发 Spring 的 {@code BeanCurrentlyInCreationException} 循环引用保护。推迟到容器完全就绪后再查库预热即可彻底规避。</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void refreshCache() {
        try {
            long totalCount = articleService.getMetricTotalCount();
            long publishedCount = articleService.getMetricPublishedCount();

            totalCountCache.set(totalCount);
            publishedCountCache.set(publishedCount);

            log.debug("文章指标缓存已更新: total={}, published={}", totalCount, publishedCount);
        } catch (Exception e) {
            log.error("刷新文章指标缓存失败", e);
        }
    }

    /**
     * 记录文章发布事件
     */
    public void recordPublish() {
        publishCounter.increment();
        // 同时更新 Gauge 缓存 (+1)
        totalCountCache.incrementAndGet();
        publishedCountCache.incrementAndGet();
    }

    /**
     * 记录文章浏览事件
     */
    public void recordView() {
        viewCounter.increment();
    }

    /**
     * 记录文章删除事件
     */
    public void recordDelete(boolean wasPublished) {
        totalCountCache.decrementAndGet();
        if (wasPublished) {
            publishedCountCache.decrementAndGet();
        }
    }
}
