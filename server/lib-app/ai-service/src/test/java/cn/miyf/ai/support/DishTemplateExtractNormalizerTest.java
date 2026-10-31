package cn.miyf.ai.support;

import cn.miyf.ai.bean.model.DishTemplateExtractResult;
import cn.miyf.ai.bean.vo.DishTemplateAiExtractVo;
import cn.miyf.kitchen.bean.dto.RecipeMaterialDto;
import cn.miyf.kitchen.bean.dto.RecipeStepDto;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 菜品模板 AI 结果规范化单元测试：覆盖裁剪、非法值清洗、待确认字段与降级。
 *
 * @author XieMingJie
 * @since 2026-10-31 18:00
 * @history 1.00 2026-10-31 18:00 XieMingJie Created.
 */
class DishTemplateExtractNormalizerTest {

    @Test
    void normalizesValidResultAndKeepsConfidence() {
        DishTemplateExtractResult raw = new DishTemplateExtractResult()
                .setName("番茄炒蛋的做法")
                .setDescription("鸡蛋滑嫩、番茄酸甜，配米饭很下饭。")
                .setServings(2)
                .setDifficulty("easy")
                .setTags(List.of("快手", "#下饭", "快手", " "))
                .setIngredients(List.of(new DishTemplateExtractResult.IngredientDraft()
                        .setName("番茄")
                        .setAmount("2")
                        .setUnit("个")
                        .setRequired(null)))
                .setSteps(List.of(new DishTemplateExtractResult.StepDraft()
                        .setTitle("炒蛋")
                        .setDescription("热油下蛋液，炒至刚凝固盛出。")
                        .setDurationSeconds(120)
                        .setTimerType("COUNTDOWN")))
                .setOverallConfidence("high")
                .setFieldConfidence(Map.of("servings", "LOW", "name", "HIGH"));

        DishTemplateAiExtractVo vo = DishTemplateExtractNormalizer.normalize(
                raw, "AI_TEXT", null, null);

        assertFalse(vo.isDegraded());
        assertNull(vo.getRejectReason());
        assertEquals("AI_TEXT", vo.getSourceType());
        assertEquals("HIGH", vo.getOverallConfidence());
        assertEquals("番茄炒蛋", vo.getDish().getName());
        assertEquals("EASY", vo.getDish().getDifficulty());
        assertEquals(List.of("快手", "下饭"), vo.getDish().getTags());
        assertEquals(120, vo.getDish().getEstimatedTotalSeconds());
        assertFalse(vo.isAiTruncated());

        RecipeMaterialDto material = vo.getDish().getIngredients().get(0);
        assertEquals("番茄", material.getName());
        assertTrue(material.getRequired());

        RecipeStepDto step = vo.getDish().getSteps().get(0);
        assertEquals(1, step.getStep());
        assertEquals("countdown", step.getTimerType());
        assertTrue(step.getCanUndo());

        assertEquals(1, vo.getUncertainFields().size());
        assertEquals("servings", vo.getUncertainFields().get(0).getPath());
        assertEquals("建议份数", vo.getUncertainFields().get(0).getLabel());
    }

    @Test
    void truncatesOverlongTextAndDropsBlankEntries() {
        String longName = "番".repeat(80);
        DishTemplateExtractResult raw = new DishTemplateExtractResult()
                .setName(longName)
                .setDescription("介".repeat(600))
                .setServings(0)
                .setDifficulty("EXTREME")
                .setIngredients(List.of(
                        new DishTemplateExtractResult.IngredientDraft().setName(" "),
                        new DishTemplateExtractResult.IngredientDraft().setName("鸡蛋").setAmount(null)))
                .setSteps(List.of(
                        new DishTemplateExtractResult.StepDraft().setDescription(""),
                        new DishTemplateExtractResult.StepDraft()
                                .setTitle("炒")
                                .setDescription("小火慢炒")
                                .setDurationSeconds(900_000)))
                .setOverallConfidence("unknown");

        DishTemplateAiExtractVo vo = DishTemplateExtractNormalizer.normalize(
                raw, "AI_TEXT", null, null);

        assertTrue(vo.isAiTruncated());
        assertEquals(60, vo.getDish().getName().length());
        assertEquals(500, vo.getDish().getDescription().length());
        assertNull(vo.getDish().getServings());
        assertNull(vo.getDish().getDifficulty());
        assertEquals(1, vo.getDish().getIngredients().size());
        assertEquals(1, vo.getDish().getSteps().size());
        assertNull(vo.getDish().getSteps().get(0).getDurationSeconds());
        assertEquals("none", vo.getDish().getSteps().get(0).getTimerType());
        assertEquals("MEDIUM", vo.getOverallConfidence());
        assertTrue(vo.getReviewHints().stream().anyMatch(h -> h.contains("缺少名称的食材")));
        assertTrue(vo.getReviewHints().stream().anyMatch(h -> h.contains("缺少说明的步骤")));
        assertTrue(vo.getReviewHints().stream().anyMatch(h -> h.contains("明显异常")));
        assertTrue(vo.getReviewHints().stream().anyMatch(h -> h.contains("未识别到时长")));
    }

    @Test
    void reportsMissingCriticalFieldsWhenNothingUsable() {
        DishTemplateExtractResult raw = new DishTemplateExtractResult()
                .setName("")
                .setOverallConfidence(null);
        Map<String, String> confidence = new LinkedHashMap<>();
        confidence.put("steps[1].durationSeconds", "MEDIUM");
        raw.setFieldConfidence(confidence);

        DishTemplateAiExtractVo vo = DishTemplateExtractNormalizer.normalize(
                raw, "AI_IMAGE", "not-a-url", "/r/1.jpg");

        assertEquals("未识别到菜品信息，请手动填写", vo.getRejectReason());
        assertEquals("LOW", vo.getOverallConfidence());
        assertEquals("AI_IMAGE", vo.getSourceType());
        assertNull(vo.getSourceUrl());
        assertEquals("/r/1.jpg", vo.getCoverImage());
        assertTrue(vo.getUncertainFields().stream().anyMatch(f -> "name".equals(f.getPath())));
        assertTrue(vo.getUncertainFields().stream()
                .anyMatch(f -> "第 2 步的时长".equals(f.getLabel())));
        assertTrue(vo.getReviewHints().stream().anyMatch(h -> h.contains("未识别到菜名")));
        assertTrue(vo.getReviewHints().stream().anyMatch(h -> h.contains("未识别到原材料")));
        assertTrue(vo.getReviewHints().stream().anyMatch(h -> h.contains("未识别到做菜步骤")));
    }

    @Test
    void keepsModelRejectReasonForNonDishMaterial() {
        DishTemplateExtractResult raw = new DishTemplateExtractResult()
                .setRejectReason("图片是风景照，未发现菜品")
                .setOverallConfidence("LOW");

        DishTemplateAiExtractVo vo = DishTemplateExtractNormalizer.normalize(
                raw, "AI_IMAGE", null, "/r/2.jpg");

        assertEquals("图片是风景照，未发现菜品", vo.getRejectReason());
        assertEquals(0, vo.getDish().getSteps().size());
        assertEquals("/r/2.jpg", vo.getCoverImage());
    }

    @Test
    void blankProducesDegradedDraftWithCover() {
        DishTemplateAiExtractVo vo = DishTemplateExtractNormalizer.blank(
                "AI 暂不可用，请手动填写菜品", true, "AI_IMAGE", null, "/r/3.jpg");

        assertTrue(vo.isDegraded());
        assertEquals("LOW", vo.getOverallConfidence());
        assertEquals("/r/3.jpg", vo.getCoverImage());
        assertNotNull(vo.getDish());
        assertTrue(vo.getReviewHints().contains("AI 暂不可用，请手动填写菜品"));
    }

    @Test
    void normalizeNullResultFallsBackToBlank() {
        DishTemplateAiExtractVo vo = DishTemplateExtractNormalizer.normalize(
                null, null, null, null);

        assertFalse(vo.isDegraded());
        assertEquals("未识别到菜品信息，请手动填写", vo.getRejectReason());
        assertEquals("AI_TEXT", vo.getSourceType());
        assertEquals("LOW", vo.getOverallConfidence());
        assertNotNull(vo.getUncertainFields());
    }
}
