-- 角色数据范围：ALL / ORG / ORG_CHILD / SELF
ALTER TABLE sys_role
    ADD COLUMN IF NOT EXISTS data_scope VARCHAR(32) NOT NULL DEFAULT 'ALL';

COMMENT ON COLUMN sys_role.data_scope IS '数据范围：ALL=全部 ORG=本组织 ORG_CHILD=本组织及下级 SELF=仅本人';

UPDATE sys_role
SET data_scope = 'ALL'
WHERE data_scope IS NULL OR data_scope = '';

-- 超级管理员保持全部数据权限
UPDATE sys_role
SET data_scope = 'ALL'
WHERE code = 'SUPER_ADMIN';
