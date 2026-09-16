-- 新预约微信订阅消息：开关/模板/跳转页/小程序版本态均在基础设置热改

INSERT INTO sys_config (id, config_key, config_value, value_type, group_code, name, description, status, sort_order, is_sensitive)
VALUES
 (41010, 'wx.subscribe.enabled', 'false', 'BOOLEAN', 'business',
  '微信订阅消息总开关',
  '开启后食客下单将向厨师推送微信订阅消息；厨师须在小程序内授权对应模板',
  'ENABLED', 80, FALSE),
 (41011, 'wx.subscribe.new_order.template_id', '', 'STRING', 'business',
  '新预约订阅消息模板ID',
  '微信公众平台「订阅消息」模板 ID；字段建议：thing1菜品 character_string2单号 thing3食客 time4时间 thing5备注',
  'ENABLED', 81, FALSE),
 (41012, 'wx.subscribe.new_order.page', 'pages/chef/orders', 'STRING', 'business',
  '新预约消息跳转页',
  '厨师点击订阅消息后打开的小程序路径',
  'ENABLED', 82, FALSE),
 (41013, 'wx.subscribe.miniprogram_state', 'formal', 'STRING', 'business',
  '订阅消息小程序版本态',
  '跳转小程序版本：formal 正式版 / trial 体验版 / developer 开发版',
  'ENABLED', 83, FALSE)
ON CONFLICT (config_key) DO NOTHING;
