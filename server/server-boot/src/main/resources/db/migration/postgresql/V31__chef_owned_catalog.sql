-- 分类/菜品/菜谱交由厨师自管：分类按厨房隔离；管理端菜单删除

ALTER TABLE dish_category
    ADD COLUMN IF NOT EXISTS kitchen_id BIGINT REFERENCES kitchen(id);

CREATE INDEX IF NOT EXISTS idx_dish_category_kitchen_id ON dish_category(kitchen_id);

-- 管理端不再维护分类 / 菜品 / 菜谱
DELETE FROM sys_menu
WHERE id IN (50101, 50102, 50103);

UPDATE sys_app
SET home_path = '/kitchen/audits'
WHERE code = 'kitchen';

