package com.blog.ai.infrastructure.ssh;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * SSH → SSE 字节流桥接器（每次 SSH 执行创建一个实例）
 *
 * <p>
 * 将 Apache MINA SSHD 的执行通道输出流（STDOUT/STDERR）实时转发到 {@link SseEmitter}，
 * 使远端 VPS 的终端日志能以 Server-Sent Events 形式逐行推送到前端浏览器。
 * </p>
 *
 * <p>工作原理：</p>
 * <ol>
 *   <li>每个字节追加到 {@link ByteBuffer} 字节缓冲区（正确处理 UTF-8 多字节字符）。</li>
 *   <li>收到换行符 {@code \n} 时，将缓冲字节解码为 UTF-8 字符串并推送到前端。</li>
 *   <li>{@link #close()} 时刷出缓冲区剩余内容，防止最后一行因无换行而丢失。</li>
 * </ol>
 *
 * <p><b>修复说明</b>：原实现使用 {@code (char) b} 逐字节强转，会将中文等多字节 UTF-8
 * 字符拆散成乱码。现改用字节级缓冲区，以完整字节序列解码，彻底修复乱码问题。</p>
 *
 * <p><b>线程安全警告</b>：此实例非线程安全，每次 SSH 执行必须创建独立实例，不可复用。</p>
 *
 * @author liusxml
 * @since 1.3.0
 */
@Slf4j
public class SseOutputStream extends OutputStream {

    /** SSE 长连接发射器，由 Controller 创建并在此处消费 */
    private final SseEmitter emitter;

    /**
     * SSE 事件名称（前端通过 {@code addEventListener(eventName, ...)} 订阅）
     * 约定值：{@code "ops_log"}
     */
    private final String eventName;

    /**
     * 字节级行缓冲区：使用字节缓冲而非 char，正确处理 UTF-8 多字节序列。
     * 初始容量 4096 字节（约 1365 个中文字符），超长行自动扩容。
     */
    private ByteBuffer lineBuffer = ByteBuffer.allocate(4096);

    /** 全局输出缓冲区：捕获执行过程中产生的所有日志 */
    private final StringBuilder allOutput = new StringBuilder(2048);

    /**
     * 创建 SSH→SSE 桥接器。
     *
     * @param emitter   当前请求的 SSE 发射器（非空）
     * @param eventName 推送给前端的 SSE 事件名称，约定为 {@code "ops_log"}
     */
    public SseOutputStream(SseEmitter emitter, String eventName) {
        this.emitter = emitter;
        this.eventName = eventName;
    }

    /**
     * 接收 SSH 管道的单个字节，积攒到字节缓冲区；遇换行符则将整行以 UTF-8 解码后推送。
     *
     * <p><b>关键修复</b>：使用字节缓冲区而非 {@code (char) b} 强转，
     * 避免 UTF-8 中文等多字节字符被截断成乱码（中文占 3 字节，直接强转只取低 8 位）。</p>
     *
     * <p>SSE 推送失败（如前端已断开连接）时仅记录 warn 日志，不抛出异常，
     * 避免因前端异常中断 SSH 命令的正常执行。</p>
     *
     * @param b SSH 管道传来的字节（int 低 8 位有效）
     */
    @Override
    public void write(int b) {
        byte byteVal = (byte) b;

        // 遇到换行符：将缓冲字节解码为 UTF-8 字符串并立即推送
        if (byteVal == '\n') {
            flushLine();
        } else {
            // 缓冲区满时自动扩容（超长日志行防御）
            if (!lineBuffer.hasRemaining()) {
                ByteBuffer larger = ByteBuffer.allocate(lineBuffer.capacity() * 2);
                lineBuffer.flip();
                larger.put(lineBuffer);
                lineBuffer = larger;
            }
            lineBuffer.put(byteVal);
        }
    }

    /**
     * 获取积累的所有终端内容（UTF-8 正确解码）
     */
    public String getAllOutput() {
        return allOutput.toString();
    }

    /**
     * 流关闭时刷出缓冲区剩余内容（处理远端命令末尾无 {@code \n} 的情况）。
     */
    @Override
    public void close() throws IOException {
        flushLine();
        super.close();
    }

    // ── 内部方法 ──────────────────────────────────────────────────────────────

    /**
     * 将字节缓冲区中的当前行以 UTF-8 解码后推送为一条 SSE 事件，然后清空缓冲区。
     * 跳过空行，避免推送无意义的空白事件。
     */
    private void flushLine() {
        lineBuffer.flip();
        byte[] bytes = new byte[lineBuffer.limit()];
        lineBuffer.get(bytes);
        lineBuffer.clear();

        // 以 UTF-8 正确解码多字节字符（修复中文乱码的核心）
        String line = new String(bytes, StandardCharsets.UTF_8);
        allOutput.append(line).append('\n');

        if (emitter != null && StringUtils.isNotBlank(line)) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(line));
            } catch (IOException e) {
                log.warn("SSE 推送失败（前端可能已断开连接）：{}", e.getMessage());
            } catch (IllegalStateException e) {
                log.warn("SSE 已关闭，忽略剩余输出：{}", e.getMessage());
            }
        }
    }
}
