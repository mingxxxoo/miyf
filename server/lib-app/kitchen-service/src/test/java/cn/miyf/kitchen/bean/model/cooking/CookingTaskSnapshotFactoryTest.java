package cn.miyf.kitchen.bean.model.cooking;

import cn.miyf.common.BusinessException;
import cn.miyf.kitchen.bean.entity.DishEntity;
import cn.miyf.kitchen.bean.entity.RecipeEntity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 做菜任务快照工厂单元测试：验证快照与来源数据隔离、步骤规范化。
 *
 * @author XieMingJie
 * @since 2026-10-31 15:00
 */
class CookingTaskSnapshotFactoryTest {

    @Test
    void snapshotIsIndependentFromSourceRecipe() {
        DishEntity dish = dish();
        RecipeEntity recipe = recipe();

        Map<String, Object> snapshot = CookingTaskSnapshotFactory.build(dish, recipe);

        recipe.getSteps().get(0).put("description", "模板被改了");
        recipe.getIngredients().get(0).put("name", "模板被改了");
        dish.setName("模板新名字");

        assertEquals("切番茄", CookingTaskSnapshotFactory.stepsOf(snapshot).get(0).get("description"));
        assertEquals("番茄", CookingTaskSnapshotFactory.ingredientsOf(snapshot).get(0).get("name"));
        assertEquals("番茄炒蛋", snapshot.get("name"));
    }

    @Test
    void normalizesStepOrderAndTimerDefaults() {
        Map<String, Object> snapshot = CookingTaskSnapshotFactory.build(dish(), recipe());

        List<Map<String, Object>> steps = CookingTaskSnapshotFactory.stepsOf(snapshot);
        assertEquals(2, steps.size());
        assertEquals(1, steps.get(0).get("order"));
        assertEquals(1, steps.get(0).get("step"));
        assertEquals(2, steps.get(1).get("order"));
        assertEquals(120, steps.get(0).get("durationSeconds"));
        assertEquals("countdown", steps.get(0).get("timerType"));
        assertEquals("none", steps.get(1).get("timerType"));
        assertEquals(true, CookingTaskSnapshotFactory.booleanOf(steps.get(0), "required", true));
    }

    @Test
    void rejectsDishWithoutSteps() {
        RecipeEntity recipe = recipe();
        recipe.setSteps(List.of());

        assertThrows(BusinessException.class,
                () -> CookingTaskSnapshotFactory.build(dish(), recipe));
    }

    @Test
    void handlesNullTagsAndIngredients() {
        DishEntity dish = dish();
        RecipeEntity recipe = recipe();
        dish.setTags(null);
        recipe.setIngredients(null);

        Map<String, Object> snapshot = CookingTaskSnapshotFactory.build(dish, recipe);

        assertTrue(((List<?>) snapshot.get("tags")).isEmpty());
        assertTrue(((List<?>) snapshot.get("ingredients")).isEmpty());
    }

    private DishEntity dish() {
        DishEntity dish = new DishEntity()
                .setName("番茄炒蛋")
                .setCoverImage("/r/1")
                .setDescription("家常菜")
                .setIsTemplate(Boolean.TRUE)
                .setTemplateStatus("published")
                .setEstimatedTotalSeconds(300);
        dish.setId(100L);
        return dish;
    }

    private RecipeEntity recipe() {
        Map<String, Object> material = new LinkedHashMap<>();
        material.put("name", "番茄");
        material.put("amount", "2");
        material.put("unit", "个");

        Map<String, Object> firstStep = new LinkedHashMap<>();
        firstStep.put("order", 9);
        firstStep.put("title", "备料");
        firstStep.put("description", "切番茄");
        firstStep.put("durationMinutes", 2);
        firstStep.put("required", true);

        Map<String, Object> secondStep = new LinkedHashMap<>();
        secondStep.put("description", "下锅翻炒");
        secondStep.put("timerType", "none");

        List<Map<String, Object>> ingredients = new ArrayList<>();
        ingredients.add(material);
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(firstStep);
        steps.add(secondStep);

        return new RecipeEntity()
                .setDishId(100L)
                .setServings(2)
                .setDifficulty("EASY")
                .setIngredients(ingredients)
                .setSteps(steps);
    }
}
