-- 健康管理菜单

INSERT INTO sys_menu (id, parent_id, name, path, component, icon, menu_type, permission_code, sort_order, visible)
VALUES (50200, NULL, '健康管理', '/health', NULL, 'HeartOutlined', 'DIR', NULL, 30, TRUE),
       (50201, 50200, '概览', '/health/overview', 'health/Overview', 'DashboardOutlined', 'MENU',
        'health:overview:view', 1, TRUE),
       (50202, 50200, '主体', '/health/subjects', 'health/Subjects', 'UserOutlined', 'MENU', 'health:subject:list', 2,
        TRUE),
       (50203, 50200, '采样', '/health/samples', 'health/Samples', 'LineChartOutlined', 'MENU', 'health:sample:list', 3,
        TRUE) ON CONFLICT (id) DO NOTHING;
