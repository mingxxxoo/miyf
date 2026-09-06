package cn.miyf.kitchen.service;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.kitchen.bean.dto.RecipeMaterialDto;
import cn.miyf.kitchen.bean.dto.RecipeSaveDto;
import cn.miyf.kitchen.bean.dto.RecipeStepDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 菜谱结构化字段校验与规范化。
 * <p>
 * 仅允许 ingredients / seasonings / steps / nutrition 约定结构，禁止金额类键名。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:35
 */
public final class RecipeStructureValidator {

    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");
    private static final Set<String> FORBIDDEN_KEYS = Set.of(
            "price", "amount_money", "money", "fee", "cost", "payment", "pay");

    private RecipeStructureValidator() {
    }

    /**
     * 校验并规范化保存请求中的结构化字段（会回写规范化后的列表）。
     *
     * @param dto 保存请求
     * @history 1.00 2026-09-04 17:35 XieMingJie Created.
     */
    public static void validateAndNormalize(RecipeSaveDto dto) {
        if (dto.getDifficulty() != null && !dto.getDifficulty().isBlank()) {
            String difficulty = dto.getDifficulty().trim().toUpperCase(Locale.ROOT);
            if (!DIFFICULTIES.contains(difficulty)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "难度仅支持 EASY/MEDIUM/HARD");
            }
            dto.setDifficulty(difficulty);
        }
        assertNonNegativeMinutes(dto.getPrepareMinutes(), "准备时间");
        assertNonNegativeMinutes(dto.getCookMinutes(), "烹饪时间");
        if (dto.getServings() != null && dto.getServings() < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "建议份量至少为 1");
        }

        dto.setIngredients(normalizeMaterials(dto.getIngredients(), "食材"));
        dto.setSeasonings(normalizeMaterials(dto.getSeasonings(), "调味料"));
        dto.setSteps(normalizeSteps(dto.getSteps()));
        dto.setNutrition(normalizeNutrition(dto.getNutrition()));
    }

    /**
     * 规范化食材/调味料列表。
     *
     * @param items 原始列表
     * @param label 文案前缀
     * @return 规范化列表
     * @history 1.00 2026-09-04 17:35 XieMingJie Created.
     */
    private static List<RecipeMaterialDto> normalizeMaterials(List<RecipeMaterialDto> items, String label) {
        if (items == null) {
            return List.of();
        }
        List<RecipeMaterialDto> normalized = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            RecipeMaterialDto item = items.get(i);
            if (item == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, label + "第 " + (i + 1) + " 项无效");
            }
            String name = trimRequired(item.getName(), label + "第 " + (i + 1) + " 项名称");
            String amount = trimRequired(item.getAmount(), label + "第 " + (i + 1) + " 项用量");
            assertNoMoneyText(name, label + "名称");
            assertNoMoneyText(amount, label + "用量");
            normalized.add(new RecipeMaterialDto().setName(name).setAmount(amount));
        }
        return normalized;
    }

    /**
     * 规范化步骤：按列表顺序重排 step，校验说明与可选图片 URL。
     *
     * @param steps 原始步骤
     * @return 规范化步骤
     * @history 1.00 2026-09-04 17:35 XieMingJie Created.
     */
    private static List<RecipeStepDto> normalizeSteps(List<RecipeStepDto> steps) {
        if (steps == null) {
            return List.of();
        }
        List<RecipeStepDto> normalized = new ArrayList<>(steps.size());
        for (int i = 0; i < steps.size(); i++) {
            RecipeStepDto step = steps.get(i);
            if (step == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "步骤第 " + (i + 1) + " 项无效");
            }
            String description = trimRequired(step.getDescription(), "步骤第 " + (i + 1) + " 项说明");
            assertNoMoneyText(description, "步骤说明");
            String title = step.getTitle() == null ? null : step.getTitle().trim();
            if (title != null && title.isEmpty()) {
                title = null;
            }
            if (title != null) {
                assertNoMoneyText(title, "步骤标题");
            }
            String image = step.getImage() == null ? null : step.getImage().trim();
            if (image != null && image.isEmpty()) {
                image = null;
            }
            if (image != null) {
                assertImageUrl(image, i + 1);
            }
            normalized.add(new RecipeStepDto()
                    .setStep(i + 1)
                    .setTitle(title)
                    .setDescription(description)
                    .setImage(image));
        }
        return normalized;
    }

    /**
     * 规范化营养信息：仅允许标量值，禁止金额类键名。
     *
     * @param nutrition 原始营养 Map
     * @return 规范化 Map，可空
     * @history 1.00 2026-09-04 17:35 XieMingJie Created.
     */
    private static Map<String, Object> normalizeNutrition(Map<String, Object> nutrition) {
        if (nutrition == null || nutrition.isEmpty()) {
            return null;
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : nutrition.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "营养信息键名不能为空");
            }
            String key = entry.getKey().trim();
            assertAllowedNutritionKey(key);
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (!(value instanceof String || value instanceof Number || value instanceof Boolean)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "营养信息仅支持文本或数值：" + key);
            }
            if (value instanceof String text) {
                assertNoMoneyText(text, "营养信息");
            }
            normalized.put(key, value);
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private static void assertNonNegativeMinutes(Integer minutes, String label) {
        if (minutes != null && minutes < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + "不能为负数");
        }
    }

    private static String trimRequired(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + "不能为空");
        }
        return value.trim();
    }

    private static void assertImageUrl(String image, int index) {
        String lower = image.toLowerCase(Locale.ROOT);
        if (!(lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("/"))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "步骤第 " + index + " 项图片地址无效");
        }
    }

    private static void assertAllowedNutritionKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        if (FORBIDDEN_KEYS.contains(lower) || lower.contains("price") || lower.contains("payment")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "营养信息不允许金额相关字段");
        }
    }

    private static void assertNoMoneyText(String text, String label) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("price") || lower.contains("payment") || lower.contains("￥") || lower.contains("¥")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + "不允许包含金额信息");
        }
    }
}
