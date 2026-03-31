package com.blog.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Ops Agent SSH 远程运维配置属性
 *
 * <p>
 * 将 {@code application.yaml} 中 {@code ops.ssh.*} 前缀的配置绑定到此类。<br>
 * 所有字段均提供合理的默认值，本地开发无需设置任何环境变量即可使用。
 * </p>
 *
 * <p>生产环境通过以下环境变量覆盖默认值：</p>
 * <pre>
 * OPS_SSH_HOST          — VPS 公网 IP
 * OPS_SSH_USER          — SSH 登录用户名（最小权限用户，非 root）
 * OPS_SSH_KEY_PATH      — 私钥文件的绝对路径
 * OPS_DEPLOY_DIR        — 远端 compose.yml 所在目录
 * OPS_SSH_CONNECT_TIMEOUT — SSH 连接超时（秒）
 * </pre>
 *
 * @author liusxml
 * @since 1.3.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ops.ssh")
public class OpsProperties {

    /**
     * 目标 VPS 公网 IP 或域名
     */
    private String host = "107.175.36.50";

    /**
     * SSH 登录用户名（专用最小权限用户，已配置 sudo docker NOPASSWD）
     */
    private String user = "blog-ops";

    /**
     * SSH 私钥文件的绝对路径
     * <ul>
     *   <li>本地开发：{@code ~/.ssh/id_ed25519}</li>
     *   <li>Docker 容器内：通过 compose volume 挂载为 {@code /app/ssh/id_ed25519}</li>
     * </ul>
     */
    private String privateKeyPath = System.getProperty("user.home") + "/.ssh/id_ed25519";

    /**
     * 远端 {@code compose.yml} 所在目录，运维脚本在此目录执行
     */
    private String deployDir = "/opt/personal-blog-deploy/personal-blog";

    /**
     * SSH 连接超时时间（单位：秒）
     */
    private int connectTimeoutSeconds = 10;
}
