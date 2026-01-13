package com.blog.comment.metrics;

import com.blog.comment.service.impl.CommentServiceImpl;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 评论模块 Micrometer 指标采集组件
 * 
 * <p>
 * 暴露以下业务指标：
 * </p>
 * <ul>
 * <li><b>blog.comments.total.count</b> - 评论总数（Gauge）</li>
 * <li><b>blog.comments.pending.count</b> - 待审核评论数（Gauge）</li>
 * <li><b>blog.comments.created</b> - 累计创建次数（Counter）</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.3.0
 */
@Slf4j
@Component
public class CommentMetrics {

    private final MeterRegistry registry;

    @Lazy // ✅ 延迟注入，打破循环依赖
    private final CommentServiceImpl commentService;

    // Gauge 缓存
    private final AtomicLong totalCountCache = new AtomicLong(0);
    private final AtomicLong pendingCountCache = new AtomicLong(0);

    // Counter 实例
    private Counter createCounter;

    /**
     * 构造函数（支持 @Lazy 注入）
     */
    public CommentMetrics(MeterRegistry registry, @Lazy CommentServiceImpl commentService) {
        this.registry = registry;
        this.commentService = commentService;
    }

    @PostConstruct
    public void init() {
        // 1. 评论总数 Gauge
        registry.gauge("blog.comments.total.count",
                totalCountCache,
                AtomicLong::get);

        // 2. 待审核评论数 Gauge
        registry.gauge("blog.comments.pending.count",
                pendingCountCache,
                AtomicLong::get);

        // 3. 评论创建 Counter
        createCounter = Counter.builder("blog.comments.created")
                .description("累计创建评论次数")
                .register(registry);

        refreshCache();

        log.info("✅ 评论模块 Micrometer 指标已启用");
    }

    /**
     * 刷新缓存
     */
    public void refreshCache() {
        try {
            long totalCount = commentService.getMetricTotalCount();
            long pendingCount = commentService.getMetricPendingCount();

            totalCountCache.set(totalCount);
            pendingCountCache.set(pendingCount);

            log.debug("评论指标缓存已更新: total={}, pending={}", totalCount, pendingCount);
        } catch (Exception e) {
            log.error("刷新评论指标缓存失败", e);
        }
    }

    public void recordCreate() {
        createCounter.increment();
        totalCountCache.incrementAndGet();
        pendingCountCache.incrementAndGet(); // 新评论默认待审核
    }

    public void recordApprove() {
        pendingCountCache.decrementAndGet();
    }

    public void recordDelete() {
        totalCountCache.decrementAndGet();
    }
}
