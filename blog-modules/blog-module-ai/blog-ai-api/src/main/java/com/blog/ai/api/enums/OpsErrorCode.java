package com.blog.ai.api.enums;

import com.blog.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Ops Agent 运维模块错误码
 *
 * <p>
 * 实现 {@link ErrorCode} 接口，提供运维操作相关的标准化错误码。<br>
 * 错误码规范：5 位数字，13xxx 段（13 = AI/Ops 模块）。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>
 * throw new BusinessException(OpsErrorCode.FORBIDDEN_SERVICE);
 * throw new BusinessException(OpsErrorCode.SSH_EXECUTION_FAILED);
 * </pre>
 *
 * @author liusxml
 * @since 1.3.0
 */
@Getter
@RequiredArgsConstructor
public enum OpsErrorCode implements ErrorCode {

    /**
     * 服务名不在白名单内
     * <p>场景：大模型传入了未授权的 serviceName，或遭遇 Prompt 注入攻击</p>
     */
    FORBIDDEN_SERVICE(13001, "Ops: 不允许操作该服务，请检查服务名是否在白名单中"),

    /**
     * SSH 连接或命令执行失败
     * <p>场景：VPS 宕机、网络超时、私钥不匹配</p>
     */
    SSH_EXECUTION_FAILED(13002, "Ops: SSH 远程命令执行失败"),

    /**
     * Webhook 签名校验失败
     * <p>场景：非 GitHub 来源的伪造请求、Secret 配置错误</p>
     */
    WEBHOOK_SIGNATURE_INVALID(13003, "Ops: GitHub Webhook 签名校验失败，请求被拒绝");

    private final Integer code;
    private final String message;
}
