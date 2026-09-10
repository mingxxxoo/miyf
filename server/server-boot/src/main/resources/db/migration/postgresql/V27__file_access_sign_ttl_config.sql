-- 文件签名 URL 有效期：管理端「基础设置」可热改，业务每次签名时读取。
INSERT INTO sys_config (id, config_key, config_value, value_type, group_code, name, description, status, sort_order, is_sensitive)
VALUES (41005, 'file.access.sign.ttl.seconds', '18000', 'NUMBER', 'upload',
        '文件签名URL有效期(秒)',
        'GET /r/{id}?exp=&sig= 的签名有效期；修改后对新签发的 URL 立即生效，已签发链接仍按其自身 exp 校验',
        'ENABLED', 50, FALSE)
ON CONFLICT (config_key) DO NOTHING;
