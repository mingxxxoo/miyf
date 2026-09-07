-- 系统应用（产品域）注册表：人员授权 Tab / 角色 product 对齐

CREATE TABLE sys_app (
    id              BIGINT       PRIMARY KEY,
    code            VARCHAR(64)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    description     VARCHAR(255),
    icon            VARCHAR(64),
    home_path       VARCHAR(255),
    sort_order      INT          NOT NULL DEFAULT 0,
    status          VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    built_in        BOOLEAN      NOT NULL DEFAULT FALSE,
    create_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_modify_time      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_sys_app_code UNIQUE (code),
    CONSTRAINT ck_sys_app_status CHECK (status IN ('ENABLED', 'DISABLED'))
);

CREATE INDEX idx_sys_app_status_sort ON sys_app (status, sort_order);

COMMENT ON TABLE sys_app IS '系统应用（产品域）';
COMMENT ON COLUMN sys_app.code IS '应用编码，与角色/权限 product 对齐';
COMMENT ON COLUMN sys_app.built_in IS '内置应用不可删除';

INSERT INTO sys_app (id, code, name, description, icon, home_path, sort_order, status, built_in)
VALUES (70001, 'kitchen', '厨房业务', '厨房预约与菜品管理', 'CoffeeOutlined', '/kitchen/categories', 10, 'ENABLED', TRUE),
       (70002, 'health', '健康管理', '健康数据与设备同步', 'HeartOutlined', '/health/overview', 20, 'ENABLED', TRUE),
       (70003, 'system', '系统', '平台与权限系统域', 'SettingOutlined', '/system', 90, 'ENABLED', TRUE)
ON CONFLICT (id) DO NOTHING;
