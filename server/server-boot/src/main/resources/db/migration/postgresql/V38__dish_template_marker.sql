-- 组织协作做菜：菜品模板（候选菜）标记与模板生命周期
-- 说明：菜品模板仍落在 dish 中，通过 is_template 标记与 template_status 四状态区分；
--      现有菜单菜品（is_template = FALSE）行为完全不变，模板不进入点菜/预约链路。

ALTER TABLE dish
    ADD COLUMN IF NOT EXISTS is_template BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS template_status VARCHAR(32),
    ADD COLUMN IF NOT EXISTS tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(32),
    ADD COLUMN IF NOT EXISTS source_url VARCHAR(1024),
    ADD COLUMN IF NOT EXISTS estimated_total_seconds INT;

-- 模板状态：draft 草稿 / pre_created 预创建 / published 已发布 / archived 已归档
ALTER TABLE dish
    DROP CONSTRAINT IF EXISTS ck_dish_template_status;
ALTER TABLE dish
    ADD CONSTRAINT ck_dish_template_status
        CHECK (template_status IS NULL
            OR template_status IN ('draft', 'pre_created', 'published', 'archived'));

-- 标记与状态必须成对出现：非模板行不写模板状态，模板行必须有状态
ALTER TABLE dish
    DROP CONSTRAINT IF EXISTS ck_dish_template_marker;
ALTER TABLE dish
    ADD CONSTRAINT ck_dish_template_marker
        CHECK ((is_template = FALSE AND template_status IS NULL)
            OR (is_template = TRUE AND template_status IS NOT NULL));

ALTER TABLE dish
    DROP CONSTRAINT IF EXISTS ck_dish_estimated_total_seconds;
ALTER TABLE dish
    ADD CONSTRAINT ck_dish_estimated_total_seconds
        CHECK (estimated_total_seconds IS NULL OR estimated_total_seconds >= 0);

CREATE INDEX IF NOT EXISTS idx_dish_template
    ON dish (kitchen_id, is_template, template_status);
CREATE INDEX IF NOT EXISTS idx_dish_template_creator
    ON dish (is_template, created_by);
