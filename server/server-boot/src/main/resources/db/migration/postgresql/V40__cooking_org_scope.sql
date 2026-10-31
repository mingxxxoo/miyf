-- 组织协作做菜：将菜品模板/做菜任务的组织归属落到 sys_org_unit。
-- 兼容策略：存量厨房一厨一组织自动建档，管理员可后续把多个厨房并入同一组织单元；
-- 新厨房在创建时由应用自动建档（见 KitchenApplicationService）。

ALTER TABLE kitchen
    ADD COLUMN IF NOT EXISTS org_unit_id BIGINT REFERENCES sys_org_unit(id);
ALTER TABLE dish
    ADD COLUMN IF NOT EXISTS org_unit_id BIGINT REFERENCES sys_org_unit(id);
ALTER TABLE cooking_task
    ADD COLUMN IF NOT EXISTS org_unit_id BIGINT REFERENCES sys_org_unit(id);

CREATE INDEX IF NOT EXISTS idx_kitchen_org_unit ON kitchen (org_unit_id);
CREATE INDEX IF NOT EXISTS idx_dish_org_template
    ON dish (org_unit_id, is_template, template_status);
CREATE INDEX IF NOT EXISTS idx_cooking_task_org_pool
    ON cooking_task (org_unit_id, status, chef_id);

-- 存量厨房：每厨创建一个组织单元，保证历史数据在管理员配置前也可用
CREATE SEQUENCE IF NOT EXISTS seq_cooking_org_backfill START WITH 900000000000000001;

CREATE TEMP TABLE tmp_kitchen_org AS
SELECT k.id AS kitchen_id,
       nextval('seq_cooking_org_backfill') AS org_id
FROM kitchen k
WHERE k.org_unit_id IS NULL;

INSERT INTO sys_org_unit (id, parent_id, code, name, sort_order, status, create_time, last_modify_time)
SELECT t.org_id,
       NULL,
       'KITCHEN_' || t.kitchen_id,
       k.name,
       0,
       'ENABLED',
       NOW(),
       NOW()
FROM tmp_kitchen_org t
JOIN kitchen k ON k.id = t.kitchen_id
ON CONFLICT (id) DO NOTHING;

UPDATE kitchen k
SET org_unit_id = t.org_id
FROM tmp_kitchen_org t
WHERE k.id = t.kitchen_id;

DROP TABLE tmp_kitchen_org;

-- 历史菜品/任务回填组织
UPDATE dish d
SET org_unit_id = k.org_unit_id
FROM kitchen k
WHERE d.kitchen_id = k.id
  AND d.org_unit_id IS NULL;

UPDATE cooking_task t
SET org_unit_id = k.org_unit_id
FROM kitchen k
WHERE t.kitchen_id = k.id
  AND t.org_unit_id IS NULL;
