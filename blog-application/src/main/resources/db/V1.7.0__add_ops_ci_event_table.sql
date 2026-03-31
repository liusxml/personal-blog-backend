-- V1.7.0: Ops CI 事件记录表
-- 存储来自 GitHub Actions Webhook（workflow_run 事件）的 CI 流水线运行记录
-- 字段路径基于 GitHub 官方 Webhook Payload 文档核查（2026-03-29）

USE blog_db;

CREATE TABLE IF NOT EXISTS `ops_ci_event`
(
    `id`            BIGINT       NOT NULL COMMENT '主键ID（雪花算法）',

    -- ── CI 业务字段 ───────────────────────────────────────────────────────────
    `repo_name`     VARCHAR(255) NOT NULL COMMENT '仓库全名，来源：repository.full_name（如 octocat/Hello-World）',
    `workflow_name` VARCHAR(255) NOT NULL COMMENT '工作流名称，来源：workflow_run.name',
    `status`        VARCHAR(50)  NOT NULL COMMENT '运行状态，来源：workflow_run.status（queued/in_progress/completed）',
    `conclusion`    VARCHAR(50)  DEFAULT NULL COMMENT '运行结论，来源：workflow_run.conclusion（运行未完成时为 NULL）；可选值：success/failure/cancelled/timed_out/action_required/skipped/stale/neutral',
    `head_sha`      VARCHAR(40)  NOT NULL COMMENT '触发提交的完整 SHA，来源：workflow_run.head_sha',
    `head_branch`   VARCHAR(255) DEFAULT NULL COMMENT '触发分支名，来源：workflow_run.head_branch（如 main）',
    `trigger_event` VARCHAR(50)  DEFAULT NULL COMMENT '触发事件类型，来源：workflow_run.event（push/pull_request/workflow_dispatch 等）',

    -- ── 公共审计字段（项目规范 4.2 七件套）────────────────────────────────────
    `version`       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by`     BIGINT       DEFAULT NULL COMMENT '创建人ID（Webhook 入库时可为空）',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（事件接收时间）',
    `update_by`     BIGINT       DEFAULT NULL COMMENT '更新人ID',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',

    PRIMARY KEY (`id`),
    KEY `idx_repo_name` (`repo_name`),
    KEY `idx_conclusion` (`conclusion`),
    KEY `idx_head_branch` (`head_branch`),
    KEY `idx_trigger_event` (`trigger_event`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'Ops CI 事件记录表（GitHub Actions workflow_run Webhook）';
