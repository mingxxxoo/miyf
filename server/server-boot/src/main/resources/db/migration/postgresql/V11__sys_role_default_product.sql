-- 角色：默认角色标记 + 产品域（人员授权 Tab 过滤）

ALTER TABLE sys_role
    ADD COLUMN IF NOT EXISTS is_default BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS product    VARCHAR(32) NOT NULL DEFAULT 'system';

COMMENT ON COLUMN sys_role.is_default IS '是否默认角色（业务层保证至多一个为 true）';
COMMENT ON COLUMN sys_role.product IS '产品域：kitchen / health / system';

UPDATE sys_role SET product = 'system' WHERE product IS NULL OR product = '';
