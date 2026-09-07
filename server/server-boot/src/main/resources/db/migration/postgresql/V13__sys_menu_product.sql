-- 菜单关联产品域，支撑应用 menuCount 精确统计
ALTER TABLE sys_menu
    ADD COLUMN IF NOT EXISTS product VARCHAR(32);

COMMENT ON COLUMN sys_menu.product IS '产品域，与 sys_app.code / 权限 product 对齐';

CREATE INDEX IF NOT EXISTS idx_sys_menu_product ON sys_menu (product);

-- 按路径 / 权限码回填
UPDATE sys_menu
SET product = 'kitchen'
WHERE product IS NULL
  AND (
        path LIKE '/kitchen%'
        OR permission_code LIKE 'kitchen:%'
        OR permission_code IN ('user:list', 'operation-log:list')
        OR id = 50100
    );

UPDATE sys_menu
SET product = 'health'
WHERE product IS NULL
  AND (
        path LIKE '/health%'
        OR permission_code LIKE 'health:%'
        OR id = 50200
    );

UPDATE sys_menu
SET product = 'system'
WHERE product IS NULL
  AND (
        path LIKE '/system%'
        OR path LIKE '/iam%'
        OR path LIKE '/dashboard%'
        OR permission_code LIKE 'iam:%'
        OR permission_code LIKE 'sys:%'
        OR id = 50010
    );

-- 子节点继承父级 product（多轮覆盖深树）
UPDATE sys_menu c
SET product = p.product
FROM sys_menu p
WHERE c.parent_id = p.id
  AND c.product IS NULL
  AND p.product IS NOT NULL;

UPDATE sys_menu c
SET product = p.product
FROM sys_menu p
WHERE c.parent_id = p.id
  AND c.product IS NULL
  AND p.product IS NOT NULL;

UPDATE sys_menu c
SET product = p.product
FROM sys_menu p
WHERE c.parent_id = p.id
  AND c.product IS NULL
  AND p.product IS NOT NULL;

-- 兜底
UPDATE sys_menu
SET product = 'system'
WHERE product IS NULL OR product = '';
