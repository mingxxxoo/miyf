-- 健康「趋势」菜单

INSERT INTO sys_menu (id, parent_id, name, path, component, icon, menu_type, permission_code, sort_order, visible)
VALUES (50205, 50200, '趋势', '/health/trends', 'health/Trends', 'FundOutlined', 'MENU', 'health:trend:view', 5,
        TRUE) ON CONFLICT (id) DO NOTHING;
