package cn.miyf.ai.support;

import cn.miyf.kitchen.bean.entity.CategoryEntity;
import cn.miyf.kitchen.bean.entity.DishEntity;
import cn.miyf.kitchen.bean.entity.RecipeEntity;
import cn.miyf.kitchen.repository.CategoryRepository;
import cn.miyf.kitchen.repository.DishRepository;
import cn.miyf.kitchen.repository.RecipeRepository;
import cn.miyf.kitchen.service.KitchenAccessService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 当前厨房在售菜单 → prompt 上下文 JSON。
 *
 * @author XieMingJie
 * @since 2026-09-17
 * @history 1.00 2026-09-17 XieMingJie Created.
 */
@Component
@RequiredArgsConstructor
public class MenuContextBuilder {

    private static final int MAX_MENU_ITEMS = 80;

    private final KitchenAccessService kitchenAccessService;
    private final DishRepository dishRepository;
    private final RecipeRepository recipeRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    /**
     * 构建绑定厨房在售菜单 JSON，并返回可校验的 dishId 集合。
     *
     * @return 菜单上下文
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    public MenuContext buildForBoundKitchen() {
        Long kitchenId = kitchenAccessService.requireBoundKitchen().getId();
        return build(kitchenId);
    }

    /**
     * 按厨房构建菜单上下文。
     *
     * @param kitchenId 厨房 ID
     * @return 菜单上下文
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    public MenuContext build(Long kitchenId) {
        List<DishEntity> dishes = dishRepository.selectList(Wrappers.<DishEntity>lambdaQuery()
                .eq(DishEntity::getKitchenId, kitchenId)
                .eq(DishEntity::getStatus, "ON_SALE")
                .eq(DishEntity::getAuditStatus, "APPROVED")
                .orderByAsc(DishEntity::getSortOrder)
                .last("LIMIT " + MAX_MENU_ITEMS));

        Map<Long, String> categoryNames = loadCategoryNames(kitchenId);
        Map<Long, RecipeEntity> recipes = loadRecipes(dishes);

        List<Map<String, Object>> menu = new ArrayList<>();
        Map<String, DishEntity> byId = new LinkedHashMap<>();
        for (DishEntity dish : dishes) {
            Map<String, Object> item = new LinkedHashMap<>();
            String dishId = String.valueOf(dish.getId());
            item.put("dishId", dishId);
            item.put("name", dish.getName());
            item.put("category", categoryNames.getOrDefault(dish.getCategoryId(), null));
            item.put("subtitle", dish.getSubtitle());
            item.put("stockType", dish.getStockType());
            item.put("stock", dish.getStock());
            RecipeEntity recipe = recipes.get(dish.getId());
            if (recipe != null && recipe.getNutrition() != null && !recipe.getNutrition().isEmpty()) {
                item.put("nutrition", recipe.getNutrition());
            }
            // 菜品暂无 tags 字段：用简介关键词占位为空数组，避免模型幻觉标签来源
            item.put("tags", List.of());
            menu.add(item);
            byId.put(dishId, dish);
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(menu);
        } catch (JsonProcessingException e) {
            json = "[]";
        }
        return new MenuContext(kitchenId, json, byId);
    }

    private Map<Long, String> loadCategoryNames(Long kitchenId) {
        return categoryRepository.selectList(Wrappers.<CategoryEntity>lambdaQuery()
                        .eq(CategoryEntity::getKitchenId, kitchenId))
                .stream()
                .filter(c -> c.getId() != null && StringUtils.hasText(c.getName()))
                .collect(Collectors.toMap(CategoryEntity::getId, CategoryEntity::getName, (a, b) -> a));
    }

    private Map<Long, RecipeEntity> loadRecipes(List<DishEntity> dishes) {
        if (dishes.isEmpty()) {
            return Map.of();
        }
        Set<Long> ids = dishes.stream().map(DishEntity::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, RecipeEntity> map = new HashMap<>();
        for (RecipeEntity recipe : recipeRepository.selectList(Wrappers.<RecipeEntity>lambdaQuery()
                .in(RecipeEntity::getDishId, ids))) {
            if (recipe.getDishId() != null) {
                map.putIfAbsent(recipe.getDishId(), recipe);
            }
        }
        return map;
    }

    /**
     * 菜单上下文。
     *
     * @param kitchenId 厨房
     * @param menuJson  注入 prompt 的 JSON
     * @param dishesById dishId → 实体
     */
    public record MenuContext(Long kitchenId, String menuJson, Map<String, DishEntity> dishesById) {
        public Set<String> dishIds() {
            return dishesById.keySet();
        }
    }
}
