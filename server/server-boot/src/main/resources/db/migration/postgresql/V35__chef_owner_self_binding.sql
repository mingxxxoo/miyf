-- 厨师默认也是食客；本厨圈自绑由应用层 ensure，此处仅回填身份标志
UPDATE kitchen_user
SET is_diner = TRUE
WHERE is_chef = TRUE
  AND COALESCE(is_diner, FALSE) = FALSE;
