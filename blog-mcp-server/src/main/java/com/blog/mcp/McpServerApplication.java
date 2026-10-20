package com.blog.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 个人博客 MCP Server 独立启动入口
 *
 * <p><b>核心作用</b>：</p>
 * <ul>
 *   <li>作为独立的 Spring Boot 进程运行（默认端口 {@code 8081}），与主应用 {@code blog-application} 解耦。</li>
 *   <li>通过 {@code spring-ai-starter-mcp-server-webmvc} 自动暴露基于 SSE 的 MCP 协议端点（{@code GET /sse}）。</li>
 *   <li>自动扫描并注册容器中声明的 {@code ToolCallbackProvider} Bean，将 Java 方法暴露为供 AI 调用的标准 MCP 工具。</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}
