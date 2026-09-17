-- AI 调用审计日志（场景、摘要、输出、token、耗时、是否采纳）

CREATE TABLE IF NOT EXISTS ai_call_log (
    id                BIGINT PRIMARY KEY,
    scene             VARCHAR(32)  NOT NULL,
    user_id           BIGINT,
    kitchen_id        BIGINT,
    model_name        VARCHAR(64),
    input_summary     VARCHAR(512),
    output_json       TEXT,
    prompt_tokens     INT,
    completion_tokens INT,
    duration_ms       BIGINT,
    success           BOOLEAN      NOT NULL DEFAULT TRUE,
    error_message     VARCHAR(512),
    adopted           BOOLEAN,
    create_time       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_call_log_scene_time ON ai_call_log (scene, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_ai_call_log_user_time ON ai_call_log (user_id, create_time DESC);
