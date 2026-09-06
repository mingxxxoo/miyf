-- 权限树：16 位业务编号、父子关系、展示路径
-- @author XieMingJie @since 2026-09-06

ALTER TABLE sys_permission
    ADD COLUMN IF NOT EXISTS perm_no VARCHAR (16),
    ADD COLUMN IF NOT EXISTS parent_id BIGINT,
    ADD COLUMN IF NOT EXISTS product VARCHAR (64),
    ADD COLUMN IF NOT EXISTS tree_name VARCHAR (256),
    ADD COLUMN IF NOT EXISTS node_type VARCHAR (16) NOT NULL DEFAULT 'API',
    ADD COLUMN IF NOT EXISTS sort_order INT NOT NULL DEFAULT 0;

COMMENT ON COLUMN sys_permission.perm_no IS '16位权限业务编号：8位组编码+8位序号';
COMMENT ON COLUMN sys_permission.parent_id IS '父权限 ID（树）';
COMMENT ON COLUMN sys_permission.product IS '产品域，如 kitchen/health';
COMMENT ON COLUMN sys_permission.tree_name IS '层级展示名：域-产品-业务-接口';
COMMENT ON COLUMN sys_permission.node_type IS 'ROOT/PRODUCT/BIZ/API';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_permission_perm_no
    ON sys_permission (perm_no) WHERE perm_no IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_sys_permission_parent
    ON sys_permission (parent_id);

ALTER TABLE sys_perm_group
    ADD COLUMN IF NOT EXISTS product VARCHAR (64);

COMMENT ON COLUMN sys_perm_group.product IS '默认产品域（来自 @PopedomGroup）';
