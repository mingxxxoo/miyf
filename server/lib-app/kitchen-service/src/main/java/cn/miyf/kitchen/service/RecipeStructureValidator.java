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
    private static final Set<String> TIMER_TYPES = Set.of("countdown", "countup", "none");
    private static final Set<String> FORBIDDEN_KEYS = Set.of(
            "price", "amount_money", "money", "fee", "cost", "payment", "pay");
    private static final int MAX_MATERIAL_TEXT = 200;
    private static final int MAX_STEP_HINT = 500;
    private static final int MAX_STEP_SECONDS = 86_400;

    private RecipeStructureValidator() {
    }

    /**
     * 校验并规范化保存请求中的结构化字段（会回写规范化后的列表）。
     *
     * @param dto 保存请求
     * @history 1.00 2026-09-04 XieMingJie Created.
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
     * @history 1.00 2026-09-04 XieMingJie Created.
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
            // 用量允许为空：AI 无法确认时按设计文档置空，由用户确认页补充，不做猜测
            String amount = trimOptional(item.getAmount(), MAX_MATERIAL_TEXT,
                    label + "第 " + (i + 1) + " 项用量");
            String unit = trimOptional(item.getUnit(), 32, label + "第 " + (i + 1) + " 项单位");
            String processing = trimOptional(item.getProcessing(), MAX_MATERIAL_TEXT,
                    label + "第 " + (i + 1) + " 项处理方式");
            String substitute = trimOptional(item.getSubstitute(), MAX_MATERIAL_TEXT,
                    label + "第 " + (i + 1) + " 项替代方案");
            String note = trimOptional(item.getNote(), MAX_MATERIAL_TEXT,
                    label + "第 " + (i + 1) + " 项备注");
            assertNoMoneyText(name, label + "名称");
            assertNoMoneyText(amount, label + "用量");
            assertNoMoneyText(processing, label + "处理方式");
            normalized.add(new RecipeMaterialDto()
                    .setName(name)
                    .setAmount(amount)
                    .setUnit(unit)
                    .setProcessing(processing)
                    .setRequired(item.getRequired() == null || item.getRequired())
                    .setSubstitute(substitute)
                    .setNote(note));
        }
        return normalized;
    }

    /**
     * 规范化步骤：按列表顺序重排 order/step，校验说明、媒体地址、时长与计时方式。
     *
     * @param steps 原始步骤
     * @return 规范化步骤
     * @history 1.00 2026-09-04 XieMingJie Created.
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
            int order = i + 1;
            String description = trimRequired(step.getDescription(), "步骤第 " + order + " 项说明");
            assertNoMoneyText(description, "步骤说明");
            String title = trimOptional(step.getTitle(), 64, "步骤第 " + order + " 项标题");
            if (title != null) {
                assertNoMoneyText(title, "步骤标题");
            }
            String image = trimOptional(step.getImage(), 1024, "步骤第 " + order + " 项图片");
            if (image != null) {
                assertMediaUrl(image, "步骤第 " + order + " 项图片");
            }
            String video = trimOptional(step.getVideo(), 1024, "步骤第 " + order + " 项视频");
            if (video != null) {
                assertMediaUrl(video, "步骤第 " + order + " 项视频");
            }
            Integer durationSeconds = step.getDurationSeconds();
            if (durationSeconds != null && (durationSeconds < 0 || durationSeconds > MAX_STEP_SECONDS)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "步骤第 " + order + " 项时长需在 0-" + MAX_STEP_SECONDS + " 秒之间");
            }
            String timerType = step.getTimerType();
            if (timerType == null || timerType.isBlank()) {
                timerType = durationSeconds != null && durationSeconds > 0 ? "countdown" : "none";
            } else {
                timerType = timerType.trim().toLowerCase(Locale.ROOT);
                if (!TIMER_TYPES.contains(timerType)) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST,
                            "步骤第 " + order + " 项计时方式仅支持 countdown/countup/none");
                }
            }
            String parallelGroup = trimOptional(step.getParallelGroup(), 32,
                    "步骤第 " + order + " 项并行分组");
            String completionHint = trimOptional(step.getCompletionHint(), MAX_STEP_HINT,
                    "步骤第 " + order + " 项完成提示");
            String warning = trimOptional(step.getWarning(), MAX_STEP_HINT,
                    "步骤第 " + order + " 项风险提示");
            normalized.add(new RecipeStepDto()
                    .setStep(order)
                    .setOrder(order)
                    .setTitle(title)
                    .setDescription(description)
                    .setImage(image)
                    .setVideo(video)
                    .setDurationSeconds(durationSeconds)
                    .setTimerType(timerType)
                    .setParallelGroup(parallelGroup)
                    .setCompletionHint(completionHint)
                    .setWarning(warning)
                    .setRequired(step.getRequired() == null || step.getRequired())
                    .setCanUndo(step.getCanUndo() == null || step.getCanUndo()));
        }
        return normalized;
    }

    /**
     * 规范化营养信息：仅允许标量值，禁止金额类键名。
     *
     * @param nutrition 原始营养 Map
     * @return 规范化 Map，可空
     * @history 1.00 2026-09-04 XieMingJie Created.
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

    /**
     * 可选文本：空串转 null，并限制长度。
     *
     * @param value 原始值
     * @param max   最大长度
     * @param label 文案前缀
     * @return 去空白后的值或 null
     */
    private static String trimOptional(String value, int max, String label) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (max > 0 && trimmed.length() > max) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + "长度不能超过 " + max);
        }
        return trimmed;
    }

    private static void assertMediaUrl(String url, String label) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (!(lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("/"))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + "地址无效");
        }
    }

    private static void assertAllowedNutritionKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        if (FORBIDDEN_KEYS.contains(lower) || lower.contains("price") || lower.contains("payment")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "营养信息不允许金额相关字段");
        }
    }

    private static void assertNoMoneyText(String text, String label) {
        if (text == null || text.isBlank()) {
            return;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("price") || lower.contains("payment") || lower.contains("￥") || lower.contains("¥")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, label + "不允许包含金额信息");
        }
    }
}
