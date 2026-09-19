-- 华为账号与微信用户共用 kitchen_user。openid 仍非空唯一，纯华为用户写入 hw: 前缀的合成值。
ALTER TABLE kitchen_user ALTER COLUMN openid TYPE VARCHAR(128);

ALTER TABLE kitchen_user
    ADD COLUMN huawei_open_id VARCHAR(128);
ALTER TABLE kitchen_user
    ADD COLUMN huawei_union_id VARCHAR(128);

CREATE UNIQUE INDEX uk_kitchen_user_huawei_union
    ON kitchen_user (huawei_union_id) WHERE huawei_union_id IS NOT NULL;

CREATE INDEX idx_kitchen_user_phone
    ON kitchen_user (phone) WHERE phone IS NOT NULL;
