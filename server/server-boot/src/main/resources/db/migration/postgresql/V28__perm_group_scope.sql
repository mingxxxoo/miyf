-- 权限组增加 scope（PERSONAL/ORG/SUPER），供鉴权与展示按配置判断，禁止依赖名称字符串。

ALTER TABLE sys_perm_group
    ADD COLUMN IF NOT EXISTS scope VARCHAR(32);

COMMENT ON COLUMN sys_perm_group.scope IS '权限域：PERSONAL / ORG / SUPER';

UPDATE sys_perm_group
SET scope = 'SUPER'
WHERE scope IS NULL;
