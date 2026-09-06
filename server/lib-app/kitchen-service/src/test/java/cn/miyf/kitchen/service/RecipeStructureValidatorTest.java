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
import static org.junit.jupiter.api.Assertions.assertThrows;

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
                        new RecipeStepDto(null, null, "cook", "/uploads/2.jpg")
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
}
