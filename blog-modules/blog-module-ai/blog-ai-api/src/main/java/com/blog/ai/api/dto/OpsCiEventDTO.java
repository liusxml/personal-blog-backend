package com.blog.ai.api.dto;

import com.blog.common.base.Identifiable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Ops CI 事件 DTO（请求输入）
 *
 * <p>
 * 由 {@code OpsWebhookController} 解析 GitHub Webhook Payload 后构建，
 * 非用户表单提交，仅作为 Service 层统一入参约定。
 * </p>
 *
 * @author liusxml
 * @since 1.3.0
 */
@Data
@Schema(description = "Ops CI 事件请求")
public class OpsCiEventDTO implements Identifiable<Long>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 事件 ID（新增时为 null）*/
    @Schema(description = "事件 ID（新增时无需传入）")
    private Long id;

    @Schema(description = "仓库全名，如 octocat/Hello-World")
    private String repoName;

    @Schema(description = "工作流名称")
    private String workflowName;

    @Schema(description = "运行状态：queued / in_progress / completed")
    private String status;

    @Schema(description = "运行结论，未完成时为 null")
    private String conclusion;

    @Schema(description = "触发 commit SHA（40字符）")
    private String headSha;

    @Schema(description = "触发分支名")
    private String headBranch;

    @Schema(description = "触发事件类型：push / pull_request / workflow_dispatch")
    private String triggerEvent;

    @Override
    public Long getId() {
        return id;
    }
}
