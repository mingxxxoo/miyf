-- 组织协作做菜：做菜任务、菜品快照与步骤计时持久化
-- 任务创建时固化 dish_snapshot，菜品模板后续修改不影响已创建/执行中的任务。

CREATE TABLE cooking_task (
    id                      BIGINT      PRIMARY KEY,
    kitchen_id              BIGINT      NOT NULL REFERENCES kitchen(id),
    dish_id                 BIGINT      NOT NULL REFERENCES dish(id),
    dish_snapshot           JSONB       NOT NULL,
    submitter_id            BIGINT      NOT NULL REFERENCES kitchen_user(id),
    chef_id                 BIGINT      REFERENCES kitchen_user(id),
    servings                INT         NOT NULL DEFAULT 1,
    note                    VARCHAR(500),
    priority                INT         NOT NULL DEFAULT 0,
    claim_mode              VARCHAR(16) NOT NULL DEFAULT 'PUBLIC',
    scheduled_start_time    TIMESTAMPTZ,
    status                  VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    current_step            INT         NOT NULL DEFAULT 0,
    started_time            TIMESTAMPTZ,
    paused_time             TIMESTAMPTZ,
    completed_time          TIMESTAMPTZ,
    total_paused_seconds    INT         NOT NULL DEFAULT 0,
    actual_duration_seconds INT,
    submit_token            VARCHAR(64),
    create_time             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modify_time        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_cooking_task_status CHECK (status IN (
        'PENDING', 'CLAIMED', 'PREPARING', 'COOKING', 'PAUSED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_cooking_task_claim_mode CHECK (claim_mode IN ('PUBLIC', 'DIRECT')),
    CONSTRAINT ck_cooking_task_servings CHECK (servings > 0),
    CONSTRAINT ck_cooking_task_priority CHECK (priority >= 0 AND priority <= 9),
    CONSTRAINT ck_cooking_task_current_step CHECK (current_step >= 0),
    CONSTRAINT ck_cooking_task_paused_nonneg CHECK (total_paused_seconds >= 0),
    CONSTRAINT ck_cooking_task_actual_nonneg CHECK (actual_duration_seconds IS NULL
        OR actual_duration_seconds >= 0)
);

-- 一次性提交令牌：同一令牌只允许落一条任务，防止重复点击/网络重试造成重复任务
CREATE UNIQUE INDEX uk_cooking_task_submit_token
    ON cooking_task (submit_token)
    WHERE submit_token IS NOT NULL;
CREATE INDEX idx_cooking_task_pool ON cooking_task (kitchen_id, status, chef_id);
CREATE INDEX idx_cooking_task_chef ON cooking_task (chef_id, status);
CREATE INDEX idx_cooking_task_dish ON cooking_task (dish_id, create_time DESC);
CREATE INDEX idx_cooking_task_submitter ON cooking_task (submitter_id, create_time DESC);

CREATE TABLE cooking_task_step (
    id                   BIGINT      PRIMARY KEY,
    task_id              BIGINT      NOT NULL REFERENCES cooking_task(id) ON DELETE CASCADE,
    step_order           INT         NOT NULL,
    status               VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    timer_type           VARCHAR(16) NOT NULL DEFAULT 'none',
    duration_seconds     INT,
    started_time         TIMESTAMPTZ,
    paused_time          TIMESTAMPTZ,
    total_paused_seconds INT         NOT NULL DEFAULT 0,
    remaining_seconds    INT,
    completed_time       TIMESTAMPTZ,
    create_time          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modify_time     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_cooking_task_step UNIQUE (task_id, step_order),
    CONSTRAINT ck_cooking_task_step_order CHECK (step_order > 0),
    CONSTRAINT ck_cooking_task_step_status CHECK (status IN ('PENDING', 'RUNNING', 'PAUSED', 'DONE')),
    CONSTRAINT ck_cooking_task_step_timer CHECK (timer_type IN ('countdown', 'countup', 'none')),
    CONSTRAINT ck_cooking_task_step_duration CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
    CONSTRAINT ck_cooking_task_step_paused_nonneg CHECK (total_paused_seconds >= 0)
);

CREATE INDEX idx_cooking_task_step_task ON cooking_task_step (task_id, step_order);
