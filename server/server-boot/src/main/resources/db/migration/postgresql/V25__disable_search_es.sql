-- 暂时停用 ES 查询：强制 search.enabled=false（业务侧亦已硬关闭召回，一律 SQL）
UPDATE sys_config
SET config_value = 'false',
    description = '暂时关闭：用户端走 SQL；恢复需部署 ES、APP_SEARCH_ENABLED=true 并改回召回开关',
    last_modify_time = NOW()
WHERE config_key = 'search.enabled';
