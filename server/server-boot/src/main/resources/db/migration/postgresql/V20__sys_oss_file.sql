-- 资源索引表（文件元数据）；物理文件以文件 ID 无后缀落盘：{namespace}/{appCode}/yyyy/MM/{id}

DROP TABLE IF EXISTS sys_oss_file;

CREATE TABLE IF NOT EXISTS sys_resource_index
(
    id               BIGINT PRIMARY KEY,
    mime_type        VARCHAR(128)  NOT NULL,
    file_name        VARCHAR(255),
    file_size        BIGINT        NOT NULL DEFAULT 0,
    create_user      BIGINT,
    last_modify_user BIGINT,
    is_compress      BOOLEAN       NOT NULL DEFAULT FALSE,
    rm_id            BIGINT,
    path             VARCHAR(512)  NOT NULL,
    is_temp          BOOLEAN       NOT NULL DEFAULT FALSE,
    source           VARCHAR(64),
    md5              CHAR(32),
    app_code         VARCHAR(64)   NOT NULL,
    access_permission VARCHAR(32)  NOT NULL DEFAULT 'PUBLIC',
    create_time      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    last_modify_time TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_resource_index_path ON sys_resource_index (path);
CREATE INDEX IF NOT EXISTS idx_sys_resource_index_app_code ON sys_resource_index (app_code);
CREATE INDEX IF NOT EXISTS idx_sys_resource_index_md5 ON sys_resource_index (md5);
CREATE INDEX IF NOT EXISTS idx_sys_resource_index_is_temp ON sys_resource_index (is_temp);
