package com.blog.ai.infrastructure.ssh;

import com.blog.ai.config.OpsProperties;
import com.blog.ai.api.enums.OpsErrorCode;
import com.blog.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.io.resource.PathResource;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Security;

import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Collection;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

/**
 * SSH 远程命令执行器
 *
 * <p>
 * 基于 Apache MINA SSHD 2.17.1，通过 ED25519 私钥认证连接远端 VPS，
 * 并将命令执行结果实时桥接到 {@link SseEmitter}（借助 {@link SseOutputStream}）。
 * </p>
 *
 * <p>生命周期：</p>
 * <ul>
 *   <li>{@link #init()} — Spring 容器启动后初始化全局 {@link SshClient}（单例，线程安全）</li>
 *   <li>{@link #destroy()} — Spring 容器关闭时优雅停止客户端，释放所有连接资源</li>
 * </ul>
 *
 * <p><b>调用约定</b>：{@link #execute(String, SseEmitter)} 会阻塞当前线程直到命令完成，
 * 调用方必须在非 HTTP 请求线程中调用（LangChain4j 的 {@code @Tool} 线程满足此要求）。</p>
 *
 * @author liusxml
 * @since 1.3.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteSshExecutor {

    private final OpsProperties opsProperties;

    /** 全局单例 SSH 客户端，由 @PostConstruct 初始化，线程安全，可复用多个 Session */
    private SshClient sshClient;

    // ── 生命周期 ───────────────────────────────────────────────────────────────

    /**
     * Spring 容器启动完成后，初始化 SshClient 并配置全局 ED25519 私钥。
     * 私钥配置在 Client 级别，所有后续 Session 自动使用，无需每次单独注入。
     */
    @PostConstruct
    public void init() {
        try {
            log.info("Ops SSH 客户端初始化，私钥路径：{}", opsProperties.getPrivateKeyPath());

            // 0. 注册 Bouncycastle Provider，启用 Ed25519 密钥支持（sshd-core 本身不内置）
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
                log.info("Ops SSH：已注册 BouncyCastle Provider（Ed25519 支持）");
            }

            // 1. 加载 ED25519 私钥（无 passphrase）
            Path keyFile = Path.of(opsProperties.getPrivateKeyPath());
            Collection<KeyPair> keyPairs = SecurityUtils.getKeyPairResourceParser()
                    .loadKeyPairs(null, new PathResource(keyFile), null);

            // 2. 创建默认客户端并配置全局密钥（官方推荐的 Client 级别全局注入方式）
            sshClient = SshClient.setUpDefaultClient();
            sshClient.setKeyIdentityProvider(
                    session -> keyPairs   // Lambda 惰性提供密钥，满足 KeyIdentityProvider 接口
            );
            sshClient.start();
            log.info("Ops SSH 客户端启动成功，加载密钥数量：{}", keyPairs.size());

        } catch (Exception e) {
            // 私钥缺失或格式错误时，给出明确的错误信息，而不是让 NPE 在后续调用时出现
            throw new IllegalStateException("Ops SSH 客户端初始化失败，请检查私钥路径配置：" + e.getMessage(), e);
        }
    }

    /**
     * Spring 容器关闭时，优雅地停止 SSH 客户端，释放所有底层网络资源。
     */
    @PreDestroy
    public void destroy() {
        if (sshClient != null && sshClient.isStarted()) {
            sshClient.stop();
            log.info("Ops SSH 客户端已停止");
        }
    }

    // ── 核心执行方法 ──────────────────────────────────────────────────────────

    /**
     * 在远端 VPS 上执行指定命令，并将 STDOUT/STDERR 实时流式推送到 SSE。
     *
     * <p>此方法会<b>阻塞当前线程</b>直到远端命令执行完毕，
     * 必须在非 HTTP 请求线程中调用（LangChain4j {@code @Tool} 执行线程满足此要求）。</p>
     *
     * @param command 要在远端执行的 Shell 命令（必须已通过 {@link OpsCommandWhitelist} 验证）
     * @param emitter 当前 SSE 连接发射器，用于向前端推送实时日志
     * @throws BusinessException 错误码 {@code SSH_EXECUTION_FAILED}，当连接或执行异常时抛出
     */
    public String execute(String command, SseEmitter emitter) {
        int timeout = opsProperties.getConnectTimeoutSeconds();
        log.info("Ops 执行 SSH 命令：host=[{}] user=[{}] command=[{}]",
                opsProperties.getHost(), opsProperties.getUser(), command);

        try (ClientSession session = sshClient
                .connect(opsProperties.getUser(), opsProperties.getHost(), 22)
                .verify(timeout, TimeUnit.SECONDS)
                .getSession()) {

            // 触发密钥认证（Client 已全局配置密钥，此处只需 verify）
            session.auth().verify(timeout, TimeUnit.SECONDS);

            try (SseOutputStream sseOut = new SseOutputStream(emitter, "ops_log");
                 ClientChannel channel = session.createExecChannel(command)) {

                // 将 SSH 的 STDOUT 和 STDERR 同时接入 SseOutputStream（统一实时推送到前端）
                channel.setOut(sseOut);
                channel.setErr(sseOut);

                channel.open().verify(timeout, TimeUnit.SECONDS);
                // 阻塞等待命令执行完成（waitFor 返回触发的事件集合）
                channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0L);

                log.info("Ops SSH 命令执行完成：command=[{}] exitStatus=[{}]",
                        command, channel.getExitStatus());
                
                return sseOut.getAllOutput();
            }

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ops SSH 执行异常：command=[{}] error=[{}]", command, e.getMessage(), e);
            throw new BusinessException(OpsErrorCode.SSH_EXECUTION_FAILED);
        }
    }
}
