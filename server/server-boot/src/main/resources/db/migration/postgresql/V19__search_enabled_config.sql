-- 搜索开关：设置中心可改；与 app.search.enabled（连接装配）配合，关闭后业务回退数据库查询
INSERT INTO sys_config (id, config_key, config_value, value_type, group_code, name, description, status, sort_order)
VALUES (41004, 'search.enabled', 'false', 'BOOLEAN', 'search', '启用 Elasticsearch 查询',
        '开启后用户端菜品列表/热门/推荐优先走 ES（需同时配置 app.search.enabled=true 并部署 ES）；关闭则回退数据库',
        'ENABLED', 40)
ON CONFLICT (config_key) DO NOTHING;
