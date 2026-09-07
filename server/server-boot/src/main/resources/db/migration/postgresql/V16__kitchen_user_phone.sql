-- 厨房微信用户：登录需录入手机号、用户名、微信号
ALTER TABLE kitchen_user
    ADD COLUMN IF NOT EXISTS phone VARCHAR(20);

ALTER TABLE kitchen_user
    ADD COLUMN IF NOT EXISTS username VARCHAR(64);

ALTER TABLE kitchen_user
    ADD COLUMN IF NOT EXISTS wechat_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_kitchen_user_phone ON kitchen_user (phone)
    WHERE phone IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_kitchen_user_username ON kitchen_user (username)
    WHERE username IS NOT NULL;
