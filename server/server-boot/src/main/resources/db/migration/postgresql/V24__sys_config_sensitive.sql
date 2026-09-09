-- 系统配置敏感标记：接口脱敏，避免密钥明文回传。

ALTER TABLE sys_config
    ADD COLUMN IF NOT EXISTS is_sensitive BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN sys_config.is_sensitive IS '敏感配置：列表/详情脱敏，更新时空值保留原值';

UPDATE sys_config
SET is_sensitive = TRUE
WHERE is_sensitive = FALSE
  AND (
        lower(group_code) LIKE '%security%'
            OR lower(config_key) LIKE '%secret%'
            OR lower(config_key) LIKE '%password%'
            OR lower(config_key) LIKE '%token%'
            OR lower(config_key) LIKE '%credential%'
            OR lower(config_key) LIKE '%webhook%'
        );


-- 重置超级管理员密码为种子默认值 change-me（无法直连库时用于找回登录）。
-- 用户名：admin；密码：change-me（与 V2__seed.sql 中 BCrypt 哈希一致）
UPDATE sys_user
SET password_hash = '$2a$10$xubni9e/V.ONXH.mSZ2kdeUUmvIF6go4fftmFtaBY2WTUS1eFEynC',
    status = 'ENABLED',
    last_modify_time = NOW()
WHERE lower(username) = 'admin';
