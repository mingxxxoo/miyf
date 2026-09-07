-- 系统设置菜单种子（业务侧栏最终隐藏，由前端 SystemLayout 静态入口承载）
-- 单位管理拆到业务根；「系统管理」整组对业务侧栏不可见

INSERT INTO sys_menu (id, parent_id, name, path, component, icon, menu_type, permission_code, sort_order, visible)
VALUES (50020, 50010, '系统配置', '/system/config', 'system/Config', 'ControlOutlined', 'MENU',
        'sys:config:list', 10, TRUE),
       (50021, 50010, '数据字典', '/system/dicts', 'system/Dicts', 'BookOutlined', 'MENU',
        'sys:dict:list', 11, TRUE),
       (50022, 50010, '通知中心', '/system/notifications', 'system/Notifications', 'BellOutlined', 'MENU',
        'sys:notify:list', 12, TRUE),
       (50023, 50010, '定时任务', '/system/jobs', 'system/Jobs', 'ScheduleOutlined', 'MENU',
        'sys:job:list', 13, TRUE),
       (50024, 50010, '系统监控', '/system/monitor', 'system/Monitor', 'MonitorOutlined', 'MENU',
        'sys:monitor:view', 14, TRUE) ON CONFLICT (id) DO NOTHING;

-- 单位管理提升为业务根菜单
UPDATE sys_menu
SET parent_id  = NULL,
    path       = '/iam/org-units',
    sort_order = 80,
    visible    = TRUE,
    icon       = 'ApartmentOutlined'
WHERE id = 50011;

-- 隐藏「系统管理」及其余子菜单（人员/角色/权限组/权限/菜单/配置等）
UPDATE sys_menu
SET visible = FALSE
WHERE id = 50010
   OR parent_id = 50010;
