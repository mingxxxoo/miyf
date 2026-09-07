-- 通知模板 + 发送流水
CREATE TABLE IF NOT EXISTS sys_notify_template (
    id               BIGINT PRIMARY KEY,
    code             VARCHAR(64)  NOT NULL,
    name             VARCHAR(128) NOT NULL,
    channel          VARCHAR(16)  NOT NULL,
    title_template   VARCHAR(256) NOT NULL,
    content_template TEXT         NOT NULL,
    status           VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    create_time       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sys_notify_template_code UNIQUE (code),
    CONSTRAINT ck_sys_notify_template_channel CHECK (channel IN ('EMAIL', 'SMS', 'INBOX')),
    CONSTRAINT ck_sys_notify_template_status CHECK (status IN ('ENABLED', 'DISABLED'))
);

COMMENT ON TABLE sys_notify_template IS '通知模板';
COMMENT ON COLUMN sys_notify_template.title_template IS '标题模板，支持变量占位符';
COMMENT ON COLUMN sys_notify_template.content_template IS '正文模板，支持变量占位符';

CREATE TABLE IF NOT EXISTS sys_notify_send_log (
    id           BIGINT PRIMARY KEY,
    template_id  BIGINT,
    channel      VARCHAR(16)  NOT NULL,
    to_key       VARCHAR(256) NOT NULL,
    title        VARCHAR(256) NOT NULL,
    content      TEXT,
    status       VARCHAR(32)  NOT NULL,
    error        TEXT,
    create_time   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_sys_notify_send_log_channel CHECK (channel IN ('EMAIL', 'SMS', 'INBOX')),
    CONSTRAINT ck_sys_notify_send_log_status CHECK (status IN ('SUCCESS', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_sys_notify_send_log_created ON sys_notify_send_log (create_time DESC);
CREATE INDEX IF NOT EXISTS idx_sys_notify_send_log_template ON sys_notify_send_log (template_id);
CREATE INDEX IF NOT EXISTS idx_sys_notify_send_log_to ON sys_notify_send_log (to_key);

COMMENT ON TABLE sys_notify_send_log IS '通知发送历史';
