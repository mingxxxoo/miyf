-- 系统配置 + 数据字典

CREATE TABLE sys_config
(
    id           BIGINT PRIMARY KEY,
    config_key   VARCHAR(128) NOT NULL,
    config_value TEXT,
    value_type   VARCHAR(16)  NOT NULL DEFAULT 'STRING',
    group_code   VARCHAR(64)  NOT NULL DEFAULT 'default',
    name         VARCHAR(128) NOT NULL,
    description  VARCHAR(255),
    status       VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    sort_order   INT          NOT NULL DEFAULT 0,
    create_time   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sys_config_key UNIQUE (config_key),
    CONSTRAINT ck_sys_config_value_type CHECK (value_type IN ('STRING', 'NUMBER', 'BOOLEAN', 'JSON')),
    CONSTRAINT ck_sys_config_status CHECK (status IN ('ENABLED', 'DISABLED'))
);

CREATE INDEX idx_sys_config_group ON sys_config (group_code);

CREATE TABLE sys_dict_type
(
    id          BIGINT PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    status      VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    sort_order  INT          NOT NULL DEFAULT 0,
    create_time  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sys_dict_type_code UNIQUE (code),
    CONSTRAINT ck_sys_dict_type_status CHECK (status IN ('ENABLED', 'DISABLED'))
);

CREATE TABLE sys_dict_item
(
    id         BIGINT PRIMARY KEY,
    type_id    BIGINT       NOT NULL REFERENCES sys_dict_type (id) ON DELETE CASCADE,
    item_value VARCHAR(128) NOT NULL,
    item_label VARCHAR(128) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    status     VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    remark     VARCHAR(255),
    create_time TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sys_dict_item UNIQUE (type_id, item_value),
    CONSTRAINT ck_sys_dict_item_status CHECK (status IN ('ENABLED', 'DISABLED'))
);

CREATE INDEX idx_sys_dict_item_type ON sys_dict_item (type_id);

-- 种子：常用配置与字典
INSERT INTO sys_config (id, config_key, config_value, value_type, group_code, name, description, status, sort_order)
VALUES (41001, 'site.name', 'miyf', 'STRING', 'site', '站点名称', '管理端展示用站点名', 'ENABLED', 10),
       (41002, 'site.login.lock.enabled', 'true', 'BOOLEAN', 'security', '登录锁定开关', '是否启用登录失败锁定',
        'ENABLED', 20),
       (41003, 'upload.max_size_mb', '10', 'NUMBER', 'upload', '上传大小上限(MB)', '管理端/业务上传单文件上限',
        'ENABLED', 30);

INSERT INTO sys_dict_type (id, code, name, description, status, sort_order)
VALUES (42001, 'common_status', '通用状态', 'ENABLED/DISABLED', 'ENABLED', 10),
       (42002, 'config_value_type', '配置值类型', '系统配置 value_type', 'ENABLED', 20);

INSERT INTO sys_dict_item (id, type_id, item_value, item_label, sort_order, status, remark)
VALUES (42101, 42001, 'ENABLED', '启用', 10, 'ENABLED', NULL),
       (42102, 42001, 'DISABLED', '禁用', 20, 'ENABLED', NULL),
       (42201, 42002, 'STRING', '字符串', 10, 'ENABLED', NULL),
       (42202, 42002, 'NUMBER', '数字', 20, 'ENABLED', NULL),
       (42203, 42002, 'BOOLEAN', '布尔', 30, 'ENABLED', NULL),
       (42204, 42002, 'JSON', 'JSON', 40, 'ENABLED', NULL);
