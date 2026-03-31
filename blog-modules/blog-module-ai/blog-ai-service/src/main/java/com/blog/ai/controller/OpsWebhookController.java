package com.blog.ai.controller;

import com.blog.ai.service.IOpsCiEventService;
import com.blog.common.exception.BusinessException;
import com.blog.common.exception.SystemErrorCode;
import com.blog.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;

/**
 * GitHub Actions Webhook 接收器
 *
 * <p>
 * 接收 GitHub {@code workflow_run} 事件，校验签名后委托 Service 解析并持久化 CI 记录。
 * </p>
 *
 * <p><b>安全机制（来自 GitHub 官方文档）</b>：</p>
 * <ol>
 *   <li>GitHub 使用 HMAC-SHA256 对 payload 签名，签名值在 {@code X-Hub-Signature-256} 请求头中，格式为 {@code sha256=<hex>}</li>
 *   <li>验签必须使用 <b>constant-time 比较</b>（{@link MessageDigest#isEqual}），禁止用 {@code .equals()}，否则存在时序攻击风险</li>
 * </ol>
 *
 * <p><b>注意</b>：此接口已在 {@code application.yaml} 的 {@code permit-all-urls} 中配置为公开接口，
 * 安全依靠 HMAC 验签保障。</p>
 *
 * @author liusxml
 * @since 1.3.0
 */
@Slf4j
@Tag(name = "Ops Webhook", description = "GitHub Actions CI 状态 Webhook 接收器")
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class OpsWebhookController {

    /** Webhook Secret，在 GitHub 仓库设置中配置，生产环境通过环境变量注入 */
    @Value("${app.ops.webhook-secret:}")
    private String webhookSecret;

    private final IOpsCiEventService opsCiEventService;

    /**
     * 接收 GitHub Actions {@code workflow_run} 事件。
     *
     * <p>处理流程：验签 → 过滤非 {@code workflow_run} 事件 → 委托 Service 解析并入库</p>
     * <p>JSON 解析逻辑在 Service 层完成，Controller 保持零 try-catch（规则 6.2）。</p>
     *
     * @param signature GitHub 发送的 HMAC-SHA256 签名（格式：{@code sha256=<hex>}）
     * @param eventType GitHub 事件类型（{@code X-GitHub-Event} 请求头）
     * @param rawBody   原始请求体字节数组（保留原始字节以确保签名校验准确）
     * @return 统一响应（规则 6.1）
     */
    @Operation(summary = "GitHub Actions Webhook", description = "接收 workflow_run 完成事件并持久化 CI 记录")
    @PostMapping("/github-actions")
    public Result<String> handleWebhook(
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "") String eventType,
            @RequestBody byte[] rawBody) {

        // ── 1. HMAC-SHA256 验签（constant-time，防时序攻击）────────────────────
        if (!verifySignature(rawBody, signature)) {
            log.warn("Webhook 签名校验失败，拒绝请求");
            throw new BusinessException(SystemErrorCode.ACCESS_DENIED, "Webhook signature mismatch");
        }

        // ── 2. 只处理 workflow_run 事件 ──────────────────────────────────────
        if (!"workflow_run".equals(eventType)) {
            log.debug("忽略非 workflow_run 事件: {}", eventType);
            return Result.success("Ignored");
        }

        // ── 3. 委托 Service 解析 Payload 并入库（JSON 解析异常在 Service 层封装为 BusinessException）──
        opsCiEventService.parseAndRecord(rawBody);

        return Result.success("OK");
    }

    // ── 内部方法 ──────────────────────────────────────────────────────────────

    /**
     * 验证 GitHub Webhook 签名（HMAC-SHA256，constant-time 比较）。
     *
     * <p>
     * 根据 GitHub 官方文档要求：<b>不能使用 {@code .equals()} 等普通字符串比较</b>，
     * 必须使用 {@link MessageDigest#isEqual} 进行 constant-time 比较，以防时序攻击。
     * </p>
     *
     * @param payload   原始请求体字节数组
     * @param signature 来自 {@code X-Hub-Signature-256} 的签名（格式：{@code sha256=<hex>}）
     * @return 签名合法返回 {@code true}，否则返回 {@code false}
     */
    private boolean verifySignature(byte[] payload, String signature) {
        if (StringUtils.isBlank(webhookSecret)) {
            log.warn("webhook-secret 未配置，跳过验签（仅限开发环境）");
            return true;
        }
        if (StringUtils.isBlank(signature) || !signature.startsWith("sha256=")) {
            return false;
        }
        String expectedHex = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, webhookSecret).hmacHex(payload);
        String expectedSig = "sha256=" + expectedHex;
        // constant-time 比较（防时序攻击，遵循 GitHub 官方文档要求）
        return MessageDigest.isEqual(
                expectedSig.getBytes(),
                signature.getBytes()
        );
    }
}
