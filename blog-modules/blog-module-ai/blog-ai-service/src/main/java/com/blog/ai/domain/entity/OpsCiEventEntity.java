package com.blog.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Ops CI 事件实体
 *
 * <p>
 * 对应表 {@code ops_ci_event}，记录来自 GitHub Actions Webhook 的
 * {@code workflow_run} 事件，用于 CI 流水线状态监控。
 * </p>
 *
 * <p>各字段 JSON 路径均已通过 GitHub 官方文档核查（2026-03-29）：</p>
 * <ul>
 *   <li>{@code repoName}     ← {@code repository.full_name}</li>
 *   <li>{@code workflowName} ← {@code workflow_run.name}</li>
 *   <li>{@code status}       ← {@code workflow_run.status}</li>
 *   <li>{@code conclusion}   ← {@code workflow_run.conclusion}（可为 null）</li>
 *   <li>{@code headSha}      ← {@code workflow_run.head_sha}</li>
 *   <li>{@code headBranch}   ← {@code workflow_run.head_branch}</li>
 *   <li>{@code triggerEvent} ← {@code workflow_run.event}</li>
 * </ul>
 *
 * @author liusxml
 * @since 1.3.0
 */
@Data
@TableName("ops_ci_event")
public class OpsCiEventEntity {

    // ── 主键 ──────────────────────────────────────────────────────────────────

    /** 主键（雪花算法，规则 4.2） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // ── CI 业务字段 ───────────────────────────────────────────────────────────

    /**
     * 仓库全名（如 {@code octocat/Hello-World}）
     * <p>来源：webhook payload 的 {@code repository.full_name}</p>
     */
    private String repoName;

    /**
     * 工作流名称（与 workflow YAML 文件的 {@code name} 字段一致）
     * <p>来源：{@code workflow_run.name}</p>
     */
    private String workflowName;

    /**
     * 运行状态：{@code queued} / {@code in_progress} / {@code completed}
     * <p>来源：{@code workflow_run.status}</p>
     */
    private String status;

    /**
     * 运行结论，当 workflow 未完成时为 {@code null}。
     * <p>可选值：{@code success} / {@code failure} / {@code cancelled} /
     * {@code timed_out} / {@code action_required} / {@code skipped}</p>
     * <p>来源：{@code workflow_run.conclusion}</p>
     */
    private String conclusion;

    /**
     * 触发 commit 的完整 SHA（40 字符）
     * <p>来源：{@code workflow_run.head_sha}</p>
     */
    private String headSha;

    /**
     * 触发分支名（如 {@code main}）
     * <p>来源：{@code workflow_run.head_branch}</p>
     */
    private String headBranch;

    /**
     * 触发事件类型：{@code push} / {@code pull_request} / {@code workflow_dispatch}
     * <p>来源：{@code workflow_run.event}</p>
     */
    private String triggerEvent;

    // ── 公共审计字段（规则 4.2 七件套）───────────────────────────────────────

    /** 乐观锁版本 */
    @Version
    private Integer version;

    /** 创建人ID */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新人ID */
    @TableField(fill = FieldFill.UPDATE)
    private Long updateBy;

    /** 更新时间 */
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0=未删除，1=已删除 */
    @TableLogic
    private Integer isDeleted;
}
