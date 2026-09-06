-- 健康「数据源」菜单（绑定 / 同步）

INSERT INTO sys_menu (id, parent_id, name, path, component, icon, menu_type, permission_code, sort_order, visible)
VALUES (50204, 50200, '数据源', '/health/providers', 'health/Providers', 'ApiOutlined', 'MENU', 'health:provider:list',
        4, TRUE) ON CONFLICT (id) DO NOTHING;
