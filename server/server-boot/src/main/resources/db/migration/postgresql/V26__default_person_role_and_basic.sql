-- 角色码按产品域唯一（各产品可有同名 default_person）
-- 基础框架产品域；种子厨房/健康个人默认角色

ALTER TABLE sys_role
    DROP CONSTRAINT IF EXISTS uk_sys_role_code;

ALTER TABLE sys_role
    DROP CONSTRAINT IF EXISTS uk_sys_role_product_code;
ALTER TABLE sys_role
    ADD CONSTRAINT uk_sys_role_product_code UNIQUE (product, code);

COMMENT ON COLUMN sys_role.product IS '产品域：kitchen / health / basic / system';

-- 注册「基础」产品域（框架 IAM / 系统设置）
INSERT INTO sys_app (id, code, name, description, icon, home_path, sort_order, status, built_in)
VALUES (70004, 'basic', '基础', '基础框架与平台能力（IAM / 系统设置）', 'AppstoreOutlined', '/system', 5, 'ENABLED',
        TRUE) ON CONFLICT (id) DO NOTHING;

-- 原 system 展示名对齐为「基础」语义（内置角色仍用 system）
UPDATE sys_app
SET name        = '基础',
    description = '基础框架与系统设置（兼容旧 product=system）'
WHERE code = 'system';

-- 厨房 / 健康：个人默认角色（权限组绑定由 PermissionBootstrap 启动时补齐）
INSERT INTO sys_role (id, code, name, description, is_default, product, data_scope)
VALUES (20010, 'default_person', '个人默认', '新用户默认角色，绑定厨房个人权限组', TRUE, 'kitchen', 'SELF'),
       (20011, 'default_person', '个人默认', '新用户默认角色，绑定健康个人权限组', TRUE, 'health',
        'SELF') ON CONFLICT (product, code) DO NOTHING;
