-- 健康模块：主体、采样、厂商账号绑定、同步任务（厂商无关）

CREATE TABLE health_subject
(
    id               BIGINT PRIMARY KEY,
    display_name     VARCHAR(128) NOT NULL,
    gender           VARCHAR(16)  NOT NULL DEFAULT 'UNKNOWN',
    birth_date       DATE,
    height_cm        NUMERIC(6, 2),
    external_user_id BIGINT,
    status           VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    remark           VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_health_subject_gender CHECK (gender IN ('UNKNOWN', 'MALE', 'FEMALE')),
    CONSTRAINT ck_health_subject_status CHECK (status IN ('ENABLED', 'DISABLED'))
);

CREATE INDEX idx_health_subject_external_user ON health_subject (external_user_id);

CREATE TABLE health_sample
(
    id               BIGINT PRIMARY KEY,
    subject_id       BIGINT         NOT NULL REFERENCES health_subject (id) ON DELETE CASCADE,
    metric_code      VARCHAR(64)    NOT NULL,
    value_num        NUMERIC(18, 6) NOT NULL,
    unit             VARCHAR(32)    NOT NULL,
    measured_at      TIMESTAMPTZ    NOT NULL,
    provider_code    VARCHAR(64)    NOT NULL DEFAULT 'manual',
    source_sample_id VARCHAR(128),
    quality          VARCHAR(16)    NOT NULL DEFAULT 'NORMAL',
    meta_json        TEXT,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_health_sample_quality CHECK (quality IN ('NORMAL', 'ESTIMATED', 'SUSPECT'))
);

CREATE INDEX idx_health_sample_subject_metric_time
    ON health_sample (subject_id, metric_code, measured_at DESC);

CREATE INDEX idx_health_sample_provider ON health_sample (provider_code);

CREATE UNIQUE INDEX uk_health_sample_provider_source
    ON health_sample (provider_code, source_sample_id) WHERE source_sample_id IS NOT NULL;

CREATE TABLE health_provider_binding
(
    id                  BIGINT PRIMARY KEY,
    subject_id          BIGINT      NOT NULL REFERENCES health_subject (id) ON DELETE CASCADE,
    provider_code       VARCHAR(64) NOT NULL,
    external_account_id VARCHAR(128),
    credential_ref      VARCHAR(255),
    status              VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_sync_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_health_provider_binding UNIQUE (subject_id, provider_code),
    CONSTRAINT ck_health_provider_binding_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'REVOKED'))
);

CREATE TABLE health_sync_run
(
    id             BIGINT PRIMARY KEY,
    provider_code  VARCHAR(64) NOT NULL,
    subject_id     BIGINT      REFERENCES health_subject (id) ON DELETE SET NULL,
    status         VARCHAR(32) NOT NULL,
    fetched_count  INT         NOT NULL DEFAULT 0,
    ingested_count INT         NOT NULL DEFAULT 0,
    error_message  VARCHAR(512),
    started_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_health_sync_run_status CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED', 'SKIPPED'))
);

CREATE INDEX idx_health_sync_run_provider_started ON health_sync_run (provider_code, started_at DESC);
