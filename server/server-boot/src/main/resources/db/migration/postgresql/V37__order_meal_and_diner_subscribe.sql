-- 预约餐次结构化字段 + 食客预约状态订阅消息配置

ALTER TABLE kitchen_order
    ADD COLUMN IF NOT EXISTS meal_date DATE,
    ADD COLUMN IF NOT EXISTS meal_type VARCHAR(32),
    ADD COLUMN IF NOT EXISTS meal_time VARCHAR(8),
    ADD COLUMN IF NOT EXISTS guest_count INT;

COMMENT ON COLUMN kitchen_order.meal_date IS '期望用餐日期';
COMMENT ON COLUMN kitchen_order.meal_type IS '餐次 BREAKFAST/LUNCH/DINNER/SNACK/OTHER';
COMMENT ON COLUMN kitchen_order.meal_time IS '期望用餐时间 HH:mm';
COMMENT ON COLUMN kitchen_order.guest_count IS '用餐人数';

INSERT INTO sys_config (id, config_key, config_value, value_type, group_code, name, description, status, sort_order, is_sensitive)
VALUES
 (41020, 'wx.subscribe.order_status.template_id', '', 'STRING', 'business',
  '预约状态订阅消息模板ID',
  '食客端：厨师推进预约状态后推送；字段建议：thing1状态 character_string2单号 thing3菜品 time4时间 thing5厨房/备注',
  'ENABLED', 84, FALSE),
 (41021, 'wx.subscribe.order_status.page', 'pages/order/index', 'STRING', 'business',
  '预约状态消息跳转页',
  '食客点击订阅消息后打开的小程序路径',
  'ENABLED', 85, FALSE)
ON CONFLICT (config_key) DO NOTHING;
