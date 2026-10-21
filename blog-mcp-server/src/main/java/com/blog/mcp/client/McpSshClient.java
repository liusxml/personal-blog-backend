package com.blog.mcp.client;

import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.io.resource.PathResource;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.Security;
import java.time.Duration;
import java.util.Collection;

/**
 * MCP 服务器只读巡检 SSH 客户端
 *
 * <p><b>核心作用</b>：</p>
 * <ul>
 *   <li>基于 Apache MINA SSHD，通过本地 Ed25519 私钥连接远端 VPS 服务器，同步执行只读巡检命令并返回文本输出。</li>
 * </ul>
 *
 * <p><b>为什么采用懒加载（Lazy Init）设计？</b></p>
 * <ul>
 *   <li>如果使用 {@code @PostConstruct} 在启动时立即加载私钥，一旦其他开发机没有配置 SSH 私钥文件，整个 MCP Server 就会启动失败。</li>
 *   <li>改为在首次调用 {@link #executeReadOnlyCommand(String)} 时才初始化 {@link SshClient}，保证即使未配私钥，也不影响启动和写文章等其他 MCP 工具。</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.0.0
 */
@Slf4j
@Component
public class McpSshClient {

    @Value("${blog.ssh.host:127.0.0.1}")
    private String host;

    @Value("${blog.ssh.port:22}")
    private int port;

    @Value("${blog.ssh.user:blog-ops}")
    private String user;

    @Value("${blog.ssh.private-key-path}")
    private String privateKeyPath;

    /**
     * 暴露远端 compose.yml 所在目录，供运维工具拼接 docker compose 巡检命令
     */
    @Getter
    @Value("${blog.ssh.deploy-dir:/opt/personal-blog-deploy/personal-blog}")
    private String deployDir;

    @Value("${blog.ssh.timeout-seconds:10}")
    private long timeoutSeconds;

    /**
     * 懒加载的全局单例 SshClient（线程安全）
     */
    private volatile SshClient sshClient;

    /**
     * 在远端 VPS 上同步执行一条只读命令并返回标准输出文本
     *
     * <p><b>技术点解析</b>：<br>
     * 使用 Apache MINA SSHD 提供的 {@link ClientSession#executeRemoteCommand(String)}
     * 高阶同步 API，一行代码即可完成建连、执行、等待完成并提取字符串结果，无需手动管理底层 Channel 流。</p>
     *
     * @param command 预置的只读 Shell 命令
     * @return 命令执行的标准输出文本
     */
    public String executeReadOnlyCommand(String command) {
        Duration timeout = Duration.ofSeconds(timeoutSeconds);
        log.info("MCP SSH 巡检: host={}:{}, user={}, command=[{}]", host, port, user, command);

        try {
            SshClient client = getOrCreateSshClient();

            // 1. 建立到远端 VPS 的 SSH 会话，并通过 try-with-resources 确保执行完毕后自动关闭会话
            try (ClientSession session = client.connect(user, host, port)
                    .verify(timeout)
                    .getSession()) {

                // 2. 执行公钥/私钥认证握手
                session.auth().verify(timeout);

                // 3. 同步执行远程命令并直接返回 UTF-8 输出结果
                return session.executeRemoteCommand(command);
            }
        } catch (Exception e) {
            log.error("MCP SSH 命令执行失败: command=[{}]", command, e);
            return "SSH 远程巡检执行失败（请确认 OPS_SSH_HOST 与私钥配置是否正确）: " + e.getMessage();
        }
    }

    /**
     * 双重检查锁定（Double-Checked Locking）懒加载初始化 SshClient
     */
    private SshClient getOrCreateSshClient() throws IOException, GeneralSecurityException {
        if (sshClient != null && sshClient.isStarted()) {
            return sshClient;
        }
        synchronized (this) {
            if (sshClient != null && sshClient.isStarted()) {
                return sshClient;
            }

            // 1. 注册 BouncyCastle 加密提供者，支持解析 OpenSSH Ed25519 格式私钥
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            // 2. 从本地磁盘加载私钥对
            Path keyFile = Path.of(privateKeyPath);
            Collection<KeyPair> keyPairs = SecurityUtils.getKeyPairResourceParser()
                    .loadKeyPairs(null, new PathResource(keyFile), null);

            // 3. 启动全局 SshClient 并绑定密钥
            SshClient client = SshClient.setUpDefaultClient();
            client.setKeyIdentityProvider(session -> keyPairs);
            client.start();

            this.sshClient = client;
            log.info("MCP SSH 客户端懒加载初始化成功: privateKeyPath={}", privateKeyPath);
            return this.sshClient;
        }
    }

    /**
     * Spring 容器关闭时优雅释放底层 NIO 线程池
     */
    @PreDestroy
    public void destroy() {
        if (sshClient != null && sshClient.isStarted()) {
            sshClient.stop();
            log.info("MCP SSH 客户端已停止");
        }
    }
}
