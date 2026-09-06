-- miyf 种子：单位、超级管理员、角色、菜单骨架、厨房分类
-- 权限/权限组由启动扫描写入；此处角色在扫描后由 Bootstrap 绑定全部权限组
-- admin 密码：change-me

INSERT INTO sys_org_unit (id, parent_id, code, name, sort_order, status) VALUES
 (10001, NULL, 'HQ', '总部', 0, 'ENABLED');

INSERT INTO sys_role (id, code, name, description) VALUES
 (20001, 'SUPER_ADMIN', '超级管理员', '拥有全部权限组'),
 (20002, 'ADMIN', '管理员', '日常运营');

INSERT INTO sys_user (id, org_unit_id, username, password_hash, nickname, status) VALUES
 (30001, 10001, 'admin',
  '$2a$10$xubni9e/V.ONXH.mSZ2kdeUUmvIF6go4fftmFtaBY2WTUS1eFEynC',
  '超级管理员', 'ENABLED');

INSERT INTO sys_user_role (id, user_id, role_id) VALUES
 (40001, 30001, 20001);

-- 菜单：平台 IAM + 厨房业务
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, menu_type, permission_code, sort_order, visible) VALUES
 (50001, NULL, '工作台', '/dashboard', 'dashboard', 'DashboardOutlined', 'MENU', 'iam:dashboard:view', 1, TRUE),
 (50010, NULL, '系统管理', '/iam', NULL, 'SettingOutlined', 'DIR', NULL, 90, TRUE),
 (50011, 50010, '单位管理', '/iam/org-units', 'iam/OrgUnits', 'ApartmentOutlined', 'MENU', 'iam:org:list', 1, TRUE),
 (50012, 50010, '人员管理', '/iam/users', 'iam/Users', 'UserOutlined', 'MENU', 'iam:user:list', 2, TRUE),
 (50013, 50010, '角色管理', '/iam/roles', 'iam/Roles', 'TeamOutlined', 'MENU', 'iam:role:list', 3, TRUE),
 (50014, 50010, '权限组', '/iam/perm-groups', 'iam/PermGroups', 'ClusterOutlined', 'MENU', 'iam:perm-group:list', 4, TRUE),
 (50015, 50010, '权限列表', '/iam/permissions', 'iam/Permissions', 'SafetyOutlined', 'MENU', 'iam:permission:list', 5, TRUE),
 (50016, 50010, '菜单管理', '/iam/menus', 'iam/Menus', 'MenuOutlined', 'MENU', 'iam:menu:list', 6, TRUE),
 (50100, NULL, '厨房业务', '/kitchen', NULL, 'CoffeeOutlined', 'DIR', NULL, 20, TRUE),
 (50101, 50100, '分类', '/kitchen/categories', 'kitchen/Categories', 'AppstoreOutlined', 'MENU', 'kitchen:category:list', 1, TRUE),
 (50102, 50100, '菜品', '/kitchen/dishes', 'kitchen/Dishes', 'ShopOutlined', 'MENU', 'kitchen:dish:list', 2, TRUE),
 (50103, 50100, '菜谱', '/kitchen/recipes', 'kitchen/Recipes', 'BookOutlined', 'MENU', 'kitchen:recipe:list', 3, TRUE),
 (50104, 50100, '预约', '/kitchen/orders', 'kitchen/Orders', 'CalendarOutlined', 'MENU', 'kitchen:order:list', 4, TRUE),
 (50105, 50100, '评价', '/kitchen/comments', 'kitchen/Comments', 'CommentOutlined', 'MENU', 'kitchen:comment:list', 5, TRUE),
 (50106, 50100, '用户', '/kitchen/users', 'kitchen/Users', 'TeamOutlined', 'MENU', 'user:list', 6, TRUE),
 (50107, 50100, '操作日志', '/kitchen/operation-logs', 'kitchen/OperationLogs', 'FileSearchOutlined', 'MENU', 'operation-log:list', 7, TRUE);

INSERT INTO dish_category (id, name, icon, sort_order, status) VALUES
 (60001, '家常热菜', 'hot', 1, 'ENABLED'),
 (60002, '清爽凉菜', 'cold', 2, 'ENABLED'),
 (60003, '汤羹粥品', 'soup', 3, 'ENABLED'),
 (60004, '主食面点', 'staple', 4, 'ENABLED');
