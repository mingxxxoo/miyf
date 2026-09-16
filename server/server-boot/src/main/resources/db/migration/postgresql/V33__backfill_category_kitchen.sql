-- 回填历史全局分类的 kitchen_id，并按厨房克隆，避免食客端分类空白
-- 注意：PostgreSQL UPDATE 目标表不能出现在 JOIN ON 中，须写在 WHERE

-- 1. 将 kitchen_id 为空的分类，按厨房各复制一份（合成 ID：原分类 id * 1000000 + 序号）
INSERT INTO dish_category (id, kitchen_id, name, icon, sort_order, status, create_time, last_modify_time)
SELECT (c.id * 1000000) + ROW_NUMBER() OVER (ORDER BY k.id, c.id),
       k.id,
       c.name,
       c.icon,
       c.sort_order,
       c.status,
       NOW(),
       NOW()
FROM dish_category c
CROSS JOIN kitchen k
WHERE c.kitchen_id IS NULL
  AND EXISTS (SELECT 1 FROM kitchen)
  AND NOT EXISTS (
      SELECT 1 FROM dish_category x
      WHERE x.kitchen_id = k.id AND x.name = c.name
  );

-- 2. 菜品挂到本厨同名分类（来自原全局分类）
UPDATE dish d
SET category_id = nc.id
FROM dish_category oldc,
     dish_category nc
WHERE d.category_id = oldc.id
  AND oldc.kitchen_id IS NULL
  AND d.kitchen_id IS NOT NULL
  AND nc.name = oldc.name
  AND nc.kitchen_id = d.kitchen_id
  AND nc.id <> oldc.id;

-- 3. 跨厨错挂：菜品厨房与分类厨房不一致时，改挂到本厨同名分类
UPDATE dish d
SET category_id = nc.id
FROM dish_category oc,
     dish_category nc
WHERE d.category_id = oc.id
  AND d.kitchen_id IS NOT NULL
  AND oc.kitchen_id IS NOT NULL
  AND oc.kitchen_id <> d.kitchen_id
  AND nc.name = oc.name
  AND nc.kitchen_id = d.kitchen_id;

-- 4. 删除已无菜品引用的历史全局分类
DELETE FROM dish_category c
WHERE c.kitchen_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.category_id = c.id)
  AND EXISTS (SELECT 1 FROM kitchen);

-- 5. 单厨兜底：若仍有空 kitchen_id 且仅一厨，直接归属
UPDATE dish_category
SET kitchen_id = (SELECT id FROM kitchen ORDER BY id ASC LIMIT 1)
WHERE kitchen_id IS NULL
  AND (SELECT COUNT(*) FROM kitchen) = 1;
