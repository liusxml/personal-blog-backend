package com.blog.mcp.config;

import com.blog.mcp.tools.BlogArticleReadTools;
import com.blog.mcp.tools.BlogArticleWriteTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 博客 MCP Server 核心配置类
 *
 * <p><b>核心作用</b>：</p>
 * <ol>
 *   <li>配置全局共享的 {@link RestClient}，用于向 {@code blog-application (8080)} 发起 HTTP 请求。</li>
 *   <li>配置 {@link ToolCallbackProvider}，将带有 {@code @Tool} 注解的业务工具类注册到 Spring AI MCP Server 中。</li>
 * </ol>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Configuration
public class BlogMcpConfig {

    /**
     * 配置调用博客主后端 (blog-application) 的 RestClient
     *
     * <p>为什么使用 {@link RestClient}？<br>
     * 它是 Spring 6 / Spring Boot 3 官方推荐的同步 HTTP 客户端，用于替代老旧的 {@code RestTemplate}，
     * 具备流式（Fluent）API 语法，代码更加简洁易读。</p>
     *
     * @param baseUrl 博客主后端地址（默认 http://localhost:8080）
     * @return 构建完成的 RestClient 实例
     */
    @Bean
    public RestClient blogRestClient(
            @Value("${blog.api.base-url:http://localhost:8080}") String baseUrl) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 设置连接超时 5 秒、读取超时 15 秒，避免后端未启动时无限阻塞 MCP 线程
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 将自定义工具类注册为 MCP Server 可暴露的 ToolCallbackProvider
     *
     * <p><b>工作原理</b>：<br>
     * {@link MethodToolCallbackProvider} 会在启动时通过反射扫描传入 Bean 中所有标记了 {@code @Tool} 的方法，
     * 提取方法名、描述和参数结构生成 MCP 标准工具定义，并在收到客户端 {@code tools/call} 请求时自动路由调用。</p>
     *
     * @param articleReadTools  博客文章只读工具集
     * @param articleWriteTools 博客文章写入与发布工具集
     * @return 工具回调提供者
     */
    @Bean
    public ToolCallbackProvider blogToolCallbackProvider(
            BlogArticleReadTools articleReadTools,
            BlogArticleWriteTools articleWriteTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(articleReadTools, articleWriteTools)
                .build();
    }
}
