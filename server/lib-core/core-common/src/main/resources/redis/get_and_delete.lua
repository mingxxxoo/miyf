-- 原子读取并删除：用于一次性消费（如 OAuth state）。
-- KEYS[1] = 目标键
-- 返回值：原值；键不存在时返回 nil
local v = redis.call('GET', KEYS[1])
if v then
  redis.call('DEL', KEYS[1])
end
return v
