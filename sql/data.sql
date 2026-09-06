-- Seed: permissions, roles, SUPER_ADMIN, sample categories
-- Password for admin: change-me (BCrypt)

INSERT INTO permissions (id, code, name, description) VALUES
 (gen_random_uuid(), 'dish:list', '菜品列表', '查看菜品'),
 (gen_random_uuid(), 'dish:create', '创建菜品', '创建菜品'),
 (gen_random_uuid(), 'dish:update', '更新菜品', '更新菜品'),
 (gen_random_uuid(), 'dish:delete', '删除菜品', '删除菜品'),
 (gen_random_uuid(), 'dish:publish', '上架菜品', '上架菜品'),
 (gen_random_uuid(), 'dish:unpublish', '下架菜品', '下架菜品'),
 (gen_random_uuid(), 'category:list', '分类列表', '查看分类'),
 (gen_random_uuid(), 'category:create', '创建分类', '创建分类'),
 (gen_random_uuid(), 'category:update', '更新分类', '更新分类'),
 (gen_random_uuid(), 'category:delete', '删除分类', '删除分类'),
 (gen_random_uuid(), 'recipe:list', '菜谱列表', '查看菜谱'),
 (gen_random_uuid(), 'recipe:create', '创建菜谱', '创建菜谱'),
 (gen_random_uuid(), 'recipe:update', '更新菜谱', '更新菜谱'),
 (gen_random_uuid(), 'recipe:delete', '删除菜谱', '删除菜谱'),
 (gen_random_uuid(), 'order:list', '预约列表', '查看预约'),
 (gen_random_uuid(), 'order:detail', '预约详情', '查看预约详情'),
 (gen_random_uuid(), 'order:update', '更新预约', '更新预约状态'),
 (gen_random_uuid(), 'order:cancel', '取消预约', '取消预约'),
 (gen_random_uuid(), 'order:complete', '完成预约', '完成预约'),
 (gen_random_uuid(), 'comment:list', '评论列表', '查看评论'),
 (gen_random_uuid(), 'comment:hide', '隐藏评论', '隐藏评论'),
 (gen_random_uuid(), 'comment:restore', '恢复评论', '恢复评论'),
 (gen_random_uuid(), 'comment:delete', '删除评论', '删除评论'),
 (gen_random_uuid(), 'user:list', '用户列表', '查看用户'),
 (gen_random_uuid(), 'user:detail', '用户详情', '查看用户详情'),
 (gen_random_uuid(), 'user:disable', '禁用用户', '禁用用户'),
 (gen_random_uuid(), 'user:enable', '启用用户', '启用用户'),
 (gen_random_uuid(), 'admin:list', '管理员列表', '查看管理员'),
 (gen_random_uuid(), 'admin:create', '创建管理员', '创建管理员'),
 (gen_random_uuid(), 'admin:update', '更新管理员', '更新管理员'),
 (gen_random_uuid(), 'admin:delete', '删除管理员', '删除管理员'),
 (gen_random_uuid(), 'role:list', '角色列表', '查看角色'),
 (gen_random_uuid(), 'role:create', '创建角色', '创建角色'),
 (gen_random_uuid(), 'role:update', '更新角色', '更新角色'),
 (gen_random_uuid(), 'role:delete', '删除角色', '删除角色'),
 (gen_random_uuid(), 'permission:list', '权限列表', '查看权限'),
 (gen_random_uuid(), 'dashboard:view', '仪表盘', '查看仪表盘'),
 (gen_random_uuid(), 'operation-log:list', '操作日志', '查看操作日志'),
 (gen_random_uuid(), 'file:upload', '文件上传', '上传文件');

INSERT INTO roles (id, code, name, description) VALUES
 (gen_random_uuid(), 'SUPER_ADMIN', '超级管理员', '拥有全部权限'),
 (gen_random_uuid(), 'ADMIN', '管理员', '日常运营权限');

-- SUPER_ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'SUPER_ADMIN';

-- ADMIN gets operational permissions (exclude admin/role/permission/operation-log management)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN'
  AND p.code NOT IN (
    'admin:list', 'admin:create', 'admin:update', 'admin:delete',
    'role:list', 'role:create', 'role:update', 'role:delete',
    'permission:list', 'operation-log:list'
  );

INSERT INTO admin_users (id, username, password_hash, nickname, status)
VALUES (
  gen_random_uuid(),
  'admin',
  '$2a$10$xubni9e/V.ONXH.mSZ2kdeUUmvIF6go4fftmFtaBY2WTUS1eFEynC',
  '超级管理员',
  'ENABLED'
);

INSERT INTO admin_user_roles (admin_user_id, role_id)
SELECT a.id, r.id FROM admin_users a, roles r
WHERE a.username = 'admin' AND r.code = 'SUPER_ADMIN';

INSERT INTO dish_category (id, name, icon, sort_order, status) VALUES
 (gen_random_uuid(), '家常热菜', 'hot', 1, 'ENABLED'),
 (gen_random_uuid(), '清爽凉菜', 'cold', 2, 'ENABLED'),
 (gen_random_uuid(), '汤品粥类', 'soup', 3, 'ENABLED'),
 (gen_random_uuid(), '主食面点', 'staple', 4, 'ENABLED');
