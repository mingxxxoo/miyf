package cn.miyf.kitchen.service;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.kitchen.bean.dto.RecipeMaterialDto;
import cn.miyf.kitchen.bean.dto.RecipeSaveDto;
import cn.miyf.kitchen.bean.dto.RecipeStepDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 菜谱结构校验单元测试。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:35
 */
class RecipeStructureValidatorTest {

    @Test
    void normalizeStepsRenumbersAndAcceptsImage() {
        RecipeSaveDto dto = new RecipeSaveDto()
                .setDishId(1L)
                .setDifficulty("easy")
                .setIngredients(List.of(new RecipeMaterialDto("jitu", "2")))
                .setSeasonings(List.of(new RecipeMaterialDto("shengchou", "2")))
                .setSteps(List.of(
                        new RecipeStepDto(9, "prep", "wash", "https://cdn.example/1.jpg"),
                        new RecipeStepDto(null, null, "cook", "/r/2")
                ));
        RecipeStructureValidator.validateAndNormalize(dto);
        assertEquals("EASY", dto.getDifficulty());
        assertEquals(1, dto.getSteps().get(0).getStep());
        assertEquals(2, dto.getSteps().get(1).getStep());
    }

    @Test
    void rejectPriceInNutrition() {
        RecipeSaveDto dto = new RecipeSaveDto()
                .setDishId(1L)
                .setNutrition(Map.of("price", 12));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> RecipeStructureValidator.validateAndNormalize(dto));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
    }

    @Test
    void rejectInvalidDifficulty() {
        RecipeSaveDto dto = new RecipeSaveDto()
                .setDishId(1L)
                .setDifficulty("SUPER");
        assertThrows(BusinessException.class, () -> RecipeStructureValidator.validateAndNormalize(dto));
    }

    @Test
    void normalizesExtendedMaterialAndStepFields() {
        RecipeSaveDto dto = new RecipeSaveDto()
                .setDishId(1L)
                .setIngredients(List.of(new RecipeMaterialDto()
                        .setName(" 五花肉 ")
                        .setAmount("500")
                        .setUnit("g")
                        .setProcessing("切块")
                        .setRequired(false)
                        .setSubstitute("梅花肉")
                        .setNote("带皮")))
                .setSteps(List.of(new RecipeStepDto()
                        .setDescription("切肉")
                        .setDurationSeconds(120)
                        .setTimerType("COUNTDOWN")
                        .setParallelGroup(" prep ")
                        .setCompletionHint("肉变色")
                        .setWarning("小心溅油")));

        RecipeStructureValidator.validateAndNormalize(dto);

        RecipeMaterialDto material = dto.getIngredients().get(0);
        assertEquals("五花肉", material.getName());
        assertEquals("g", material.getUnit());
        assertEquals("切块", material.getProcessing());
        assertFalse(material.getRequired());
        assertEquals("梅花肉", material.getSubstitute());
        assertEquals("带皮", material.getNote());

        RecipeStepDto step = dto.getSteps().get(0);
        assertEquals(1, step.getOrder());
        assertEquals(1, step.getStep());
        assertEquals(120, step.getDurationSeconds());
        assertEquals("countdown", step.getTimerType());
        assertEquals("prep", step.getParallelGroup());
        assertTrue(step.getRequired());
        assertTrue(step.getCanUndo());
    }

    @Test
    void rejectsUnknownTimerType() {
        RecipeSaveDto dto = new RecipeSaveDto()
                .setDishId(1L)
                .setSteps(List.of(new RecipeStepDto()
                        .setDescription("炖煮")
                        .setDurationSeconds(600)
                        .setTimerType("alarm")));
        assertThrows(BusinessException.class, () -> RecipeStructureValidator.validateAndNormalize(dto));
    }

    @Test
    void rejectsStepDurationBeyondOneDay() {
        RecipeSaveDto dto = new RecipeSaveDto()
                .setDishId(1L)
                .setSteps(List.of(new RecipeStepDto()
                        .setDescription("腌制")
                        .setDurationSeconds(86_401)));
        assertThrows(BusinessException.class, () -> RecipeStructureValidator.validateAndNormalize(dto));
    }
}
