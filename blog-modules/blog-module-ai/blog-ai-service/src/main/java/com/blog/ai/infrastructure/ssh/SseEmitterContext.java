package com.blog.ai.infrastructure.ssh;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE Emitter 上下文管理器，用于在多线程或框架代理场景下传递 SSE Emitter 实例
 */
public class SseEmitterContext {
    private static final Map<String, SseEmitter> CONTEXT = new ConcurrentHashMap<>();

    public static void put(String sessionId, SseEmitter emitter) {
        CONTEXT.put(sessionId, emitter);
    }

    public static SseEmitter get(String sessionId) {
        return CONTEXT.get(sessionId);
    }

    public static void remove(String sessionId) {
        CONTEXT.remove(sessionId);
    }
}
