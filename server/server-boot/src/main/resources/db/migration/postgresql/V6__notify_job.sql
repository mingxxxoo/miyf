-- 站内信落库 + 定时任务启停状态

CREATE TABLE sys_inbox_message
(
    id         BIGINT PRIMARY KEY,
    user_key   VARCHAR(128) NOT NULL,
    title      VARCHAR(256) NOT NULL,
    content    TEXT,
    read_flag  BOOLEAN      NOT NULL DEFAULT FALSE,
    create_time TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sys_inbox_user_created ON sys_inbox_message (user_key, create_time DESC);
CREATE INDEX idx_sys_inbox_user_unread ON sys_inbox_message (user_key, read_flag) WHERE read_flag = FALSE;

CREATE TABLE sys_job_state
(
    code       VARCHAR(128) PRIMARY KEY,
    enabled    BOOLEAN     NOT NULL DEFAULT TRUE,
    last_modify_time TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
