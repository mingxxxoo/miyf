-- 华为账号与微信用户共用 kitchen_user。openid 仍非空唯一，纯华为用户写入 hw: 前缀的合成值。
ALTER TABLE kitchen_user ALTER COLUMN openid TYPE VARCHAR(128);

ALTER TABLE kitchen_user
    ADD COLUMN IF NOT EXISTS huawei_open_id VARCHAR(128);
ALTER TABLE kitchen_user
    ADD COLUMN IF NOT EXISTS huawei_union_id VARCHAR(128);

CREATE UNIQUE INDEX IF NOT EXISTS uk_kitchen_user_huawei_union
    ON kitchen_user (huawei_union_id) WHERE huawei_union_id IS NOT NULL;

-- phone 局部索引已在 V16 创建，此处不再重复 CREATE。
