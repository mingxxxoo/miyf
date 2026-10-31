package cn.miyf.kitchen.bean.model.cooking;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.kitchen.bean.entity.DishEntity;
import cn.miyf.kitchen.bean.entity.RecipeEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 做菜任务快照工厂。
 * <p>
 * 创建任务时把菜品模板与菜谱固化为独立 JSON 快照；快照中的原料、步骤均为深拷贝，
 * 菜品模板后续修改或删除不会影响已创建的任务。
 *
 * @author XieMingJie
 * @since 2026-10-31 15:00
 * @history 1.00 2026-10-31 15:00 XieMingJie Created.
 */
public final class CookingTaskSnapshotFactory {

    private static final Set<String> TIMER_TYPES = Set.of("countdown", "countup", "none");

    private CookingTaskSnapshotFactory() {
    }

    /**
     * 构建菜品快照；菜谱缺少步骤时拒绝创建任务。
     *
     * @param dish   菜品模板
     * @param recipe 菜谱，可为空
     * @return 可写入 JSONB 的快照
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    public static Map<String, Object> build(DishEntity dish, RecipeEntity recipe) {
        if (dish == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜品不存在");
        }
        List<Map<String, Object>> steps = copySteps(recipe == null ? null : recipe.getSteps());
        if (steps.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "菜品尚未配置做菜步骤，无法创建任务");
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("dishId", dish.getId());
        snapshot.put("name", dish.getName());
        snapshot.put("coverImage", dish.getCoverImage());
        snapshot.put("description", dish.getDescription());
        snapshot.put("tags", dish.getTags() == null ? List.of() : List.copyOf(dish.getTags()));
        snapshot.put("difficulty", recipe == null ? null : recipe.getDifficulty());
        snapshot.put("servings", recipe == null ? null : recipe.getServings());
        snapshot.put("estimatedTotalSeconds", dish.getEstimatedTotalSeconds());
        snapshot.put("sourceType", dish.getSourceType());
        snapshot.put("sourceUrl", dish.getSourceUrl());
        snapshot.put("ingredients", copyMaps(recipe == null ? null : recipe.getIngredients()));
        snapshot.put("seasonings", copyMaps(recipe == null ? null : recipe.getSeasonings()));
        snapshot.put("steps", steps);
        snapshot.put("capturedAt", Instant.now().toString());
        return snapshot;
    }

    /**
     * 读取快照中的步骤列表。
     *
     * @param snapshot 快照
     * @return 步骤副本列表
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    public static List<Map<String, Object>> stepsOf(Map<String, Object> snapshot) {
        if (snapshot == null) {
            return List.of();
        }
        Object value = snapshot.get("steps");
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> steps = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                steps.add(copyMap(map));
            }
        }
        return steps;
    }

    /**
     * 读取快照中的原材料列表。
     *
     * @param snapshot 快照
     * @return 原材料副本列表
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    public static List<Map<String, Object>> ingredientsOf(Map<String, Object> snapshot) {
        if (snapshot == null) {
            return List.of();
        }
        Object value = snapshot.get("ingredients");
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> items = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                items.add(copyMap(map));
            }
        }
        return items;
    }

    /**
     * 从快照读取文本字段，去空白后为空返回 null。
     *
     * @param map 数据
     * @param key 字段
     * @return 文本
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    public static String textOf(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * 从快照读取整数字段，非法值返回 null。
     *
     * @param map 数据
     * @param key 字段
     * @return 整数
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    public static Integer intOf(Map<String, Object> map, String key) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从快照读取布尔字段，兼容 JSON 中字符串形式的 true/false。
     *
     * @param map          数据
     * @param key          字段
     * @param defaultValue 缺省值
     * @return 布尔值
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    public static boolean booleanOf(Map<String, Object> map, String key, boolean defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : Boolean.parseBoolean(text);
    }

    private static List<Map<String, Object>> copySteps(List<Map<String, Object>> steps) {
        if (steps == null || steps.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>(steps.size());
        int order = 1;
        for (Map<String, Object> step : steps) {
            if (step == null) {
                continue;
            }
            Map<String, Object> copy = copyMap(step);
            copy.put("order", order);
            copy.put("step", order);
            if (copy.get("description") == null) {
                copy.put("description", "");
            }
            Integer duration = intOf(copy, "durationSeconds");
            if (duration == null) {
                Integer minutes = intOf(copy, "durationMinutes");
                duration = minutes == null ? null : minutes * 60;
            }
            copy.put("durationSeconds", duration);
            copy.put("timerType", normalizeTimerType(copy.get("timerType"), duration));
            result.add(copy);
            order++;
        }
        return result;
    }

    private static String normalizeTimerType(Object value, Integer durationSeconds) {
        String timerType = value == null ? null : String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        if (timerType == null || timerType.isEmpty() || !TIMER_TYPES.contains(timerType)) {
            return durationSeconds != null && durationSeconds > 0 ? "countdown" : "none";
        }
        return timerType;
    }

    private static List<Map<String, Object>> copyMaps(List<Map<String, Object>> maps) {
        if (maps == null || maps.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>(maps.size());
        for (Map<String, Object> map : maps) {
            if (map != null) {
                result.add(copyMap(map));
            }
        }
        return result;
    }

    private static Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(String.valueOf(key), value));
        return copy;
    }
}
