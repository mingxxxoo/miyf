-- 仅当当前值等于 ARGV[1] 时删除键（分布式锁安全释放）。
-- KEYS[1] = 锁键
-- ARGV[1] = 持有者 token
-- 返回值：1 已删除；0 未匹配或键不存在
if redis.call('get', KEYS[1]) == ARGV[1] then
  return redis.call('del', KEYS[1])
end
return 0
