package com.blog.ai.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Ops CI 事件 VO（对外响应视图）
 *
 * @author liusxml
 * @since 1.3.0
 */
@Data
@Schema(description = "CI 事件响应")
public class OpsCiEventVO {

    @Schema(description = "事件 ID")
    private Long id;

    @Schema(description = "仓库全名，如 octocat/Hello-World")
    private String repoName;

    @Schema(description = "工作流名称")
    private String workflowName;

    @Schema(description = "运行状态：queued / in_progress / completed")
    private String status;

    @Schema(description = "运行结论：success / failure / cancelled 等，未完成时为 null")
    private String conclusion;

    @Schema(description = "触发 commit SHA（40字符）")
    private String headSha;

    @Schema(description = "触发分支名")
    private String headBranch;

    @Schema(description = "触发事件类型：push / pull_request / workflow_dispatch")
    private String triggerEvent;

    @Schema(description = "事件接收时间")
    private LocalDateTime createTime;
}
