package cn.miyf.ai.support;

import cn.miyf.ai.bean.model.DishTemplateExtractResult;
import cn.miyf.ai.bean.vo.DishTemplateAiExtractVo;
import cn.miyf.kitchen.bean.dto.RecipeMaterialDto;
import cn.miyf.kitchen.bean.dto.RecipeStepDto;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 菜品模板 AI 结果规范化。
 * <p>
 * 纯函数：不落库、不调用模型。负责把模型输出裁剪到保存接口可接受的范围内，
 * 并按「缺失关键字段 + 模型自报低置信」生成待确认字段与核对提示，
 * 保证 AI 结果只能作为草稿，用户确认前不会被自动发布。
 *
 * @author XieMingJie
 * @since 2026-10-31 18:00
 * @history 1.00 2026-10-31 18:00 XieMingJie Created.
 */
public final class DishTemplateExtractNormalizer {

    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");
    private static final Set<String> TIMER_TYPES = Set.of("countdown", "countup", "none");
    private static final Set<String> CONFIDENCES = Set.of("HIGH", "MEDIUM", "LOW");
    private static final Pattern INDEXED_PATH =
            Pattern.compile("^(ingredients|steps)\\[(\\d+)](?:\\.([A-Za-z]+))?$");

    private static final int MAX_NAME = 60;
    private static final int MAX_DESCRIPTION = 500;
    private static final int MAX_STEP_DESCRIPTION = 300;
    private static final int MAX_STEP_TITLE = 64;
    private static final int MAX_MATERIAL_TEXT = 200;
    private static final int MAX_UNIT = 32;
    private static final int MAX_HINT = 500;
    private static final int MAX_TAGS = 6;
    private static final int MAX_TAG_LENGTH = 16;
    private static final int MAX_INGREDIENTS = 40;
    private static final int MAX_STEPS = 30;
    private static final int MAX_STEP_SECONDS = 86_400;
    private static final int MAX_UNCERTAIN_FIELDS = 20;

    private static final String REVIEW_HEAD = "AI 识别结果仅供参考，请逐项核对后保存";
    private static final String EMPTY_DISH = "未识别到菜品信息，请手动填写";

    private DishTemplateExtractNormalizer() {
    }

    /**
     * 把模型输出规范化为确认页 VO。
     *
     * @param raw       模型原始输出，可空
     * @param sourceType 来源：AI_TEXT / AI_IMAGE / AI_URL
     * @param sourceUrl  来源网页地址，可空
     * @param coverImage 封面图 URL，可空
     * @return 确认页 VO，永不返回 null
     * @history 1.00 2026-10-31 18:00 XieMingJie Created.
     */
    public static DishTemplateAiExtractVo normalize(DishTemplateExtractResult raw,
                                                    String sourceType,
                                                    String sourceUrl,
                                                    String coverImage) {
        if (raw == null) {
            return blank(EMPTY_DISH, false, sourceType, sourceUrl, coverImage);
        }
        Stats stats = new Stats();
        String name = normalizeName(raw.getName(), stats);
        String description = truncate(clean(raw.getDescription()), MAX_DESCRIPTION, stats);
        Integer servings = normalizeServings(raw.getServings());
        String difficulty = normalizeDifficulty(raw.getDifficulty());
        List<String> tags = normalizeTags(raw.getTags());
        List<RecipeMaterialDto> ingredients = normalizeIngredients(raw.getIngredients(), stats);
        List<RecipeStepDto> steps = normalizeSteps(raw.getSteps(), stats);
        String rejectReason = clean(raw.getRejectReason());
        if (rejectReason == null && name == null && steps.isEmpty()) {
            rejectReason = EMPTY_DISH;
        }
        Map<String, DishTemplateAiExtractVo.UncertainFieldVo> uncertain = collectUncertain(raw, name,
                description, servings, difficulty, ingredients, steps);
        String overallConfidence = normalizeOverall(raw.getOverallConfidence(), name, steps);

        DishTemplateAiExtractVo.DishDraftVo dish = new DishTemplateAiExtractVo.DishDraftVo()
                .setName(name)
                .setDescription(description)
                .setServings(servings)
                .setDifficulty(difficulty)
                .setTags(tags)
                .setIngredients(ingredients)
                .setSteps(steps)
                .setEstimatedTotalSeconds(sumDurations(steps));

        return new DishTemplateAiExtractVo()
                .setDegraded(false)
                .setRejectReason(rejectReason)
                .setSourceType(normalizeSourceType(sourceType))
                .setSourceUrl(normalizeSourceUrl(sourceUrl))
                .setCoverImage(clean(coverImage))
                .setOverallConfidence(overallConfidence)
                .setDish(dish)
                .setUncertainFields(new ArrayList<>(uncertain.values()))
                .setReviewHints(buildHints(stats, name, description, ingredients, steps, overallConfidence,
                        rejectReason))
                .setAiTruncated(stats.truncated);
    }

    /**
     * 生成降级/空草稿：保留已上传封面，方便用户直接手动补录。
     *
     * @param reason     降级原因
     * @param degraded   是否因 AI 不可用降级
     * @param sourceType 来源
     * @param sourceUrl  来源网页地址
     * @param coverImage 封面图 URL
     * @return 空草稿 VO
     * @history 1.00 2026-10-31 18:00 XieMingJie Created.
     */
    public static DishTemplateAiExtractVo blank(String reason,
                                                boolean degraded,
                                                String sourceType,
                                                String sourceUrl,
                                                String coverImage) {
        List<String> hints = new ArrayList<>();
        hints.add(clean(reason) == null ? EMPTY_DISH : reason);
        hints.add("可以先上传图片作为封面，再手动填写菜品");
        return new DishTemplateAiExtractVo()
                .setDegraded(degraded)
                .setRejectReason(clean(reason) == null ? EMPTY_DISH : reason)
                .setSourceType(normalizeSourceType(sourceType))
                .setSourceUrl(normalizeSourceUrl(sourceUrl))
                .setCoverImage(clean(coverImage))
                .setOverallConfidence("LOW")
                .setDish(new DishTemplateAiExtractVo.DishDraftVo())
                .setUncertainFields(new ArrayList<>())
                .setReviewHints(hints)
                .setAiTruncated(false);
    }

    private static String normalizeName(String value, Stats stats) {
        String name = clean(value);
        if (name == null) {
            return null;
        }
        name = name.replaceAll("\\s+", " ");
        name = name.replaceAll("(的做法|做法|菜谱|食谱)$", "").trim();
        if (name.isEmpty()) {
            return null;
        }
        return truncate(name, MAX_NAME, stats);
    }

    private static Integer normalizeServings(Integer servings) {
        if (servings == null || servings < 1 || servings > 99) {
            return null;
        }
        return servings;
    }

    private static String normalizeDifficulty(String difficulty) {
        String value = clean(difficulty);
        if (value == null) {
            return null;
        }
        String upper = value.toUpperCase(Locale.ROOT);
        return DIFFICULTIES.contains(upper) ? upper : null;
    }

    private static List<String> normalizeTags(List<String> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String raw : rawTags) {
            String tag = clean(raw);
            if (tag == null) {
                continue;
            }
            tag = tag.replaceFirst("^#+", "").trim();
            if (tag.isEmpty()) {
                continue;
            }
            if (tag.length() > MAX_TAG_LENGTH) {
                tag = tag.substring(0, MAX_TAG_LENGTH);
            }
            unique.add(tag);
            if (unique.size() >= MAX_TAGS) {
                break;
            }
        }
        return new ArrayList<>(unique);
    }

    private static List<RecipeMaterialDto> normalizeIngredients(List<DishTemplateExtractResult.IngredientDraft> raw,
                                                                Stats stats) {
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        List<RecipeMaterialDto> list = new ArrayList<>();
        for (DishTemplateExtractResult.IngredientDraft item : raw) {
            if (list.size() >= MAX_INGREDIENTS) {
                break;
            }
            if (item == null) {
                continue;
            }
            String name = truncate(clean(item.getName()), MAX_MATERIAL_TEXT, stats);
            if (name == null) {
                stats.droppedIngredients++;
                continue;
            }
            String amount = truncate(clean(item.getAmount()), MAX_MATERIAL_TEXT, stats);
            if (amount == null) {
                stats.missingAmounts++;
            }
            list.add(new RecipeMaterialDto()
                    .setName(name)
                    .setAmount(amount)
                    .setUnit(truncate(clean(item.getUnit()), MAX_UNIT, stats))
                    .setProcessing(truncate(clean(item.getProcessing()), MAX_MATERIAL_TEXT, stats))
                    .setRequired(item.getRequired() == null || item.getRequired())
                    .setSubstitute(truncate(clean(item.getSubstitute()), MAX_MATERIAL_TEXT, stats))
                    .setNote(truncate(clean(item.getNote()), MAX_MATERIAL_TEXT, stats)));
        }
        return list;
    }

    private static List<RecipeStepDto> normalizeSteps(List<DishTemplateExtractResult.StepDraft> raw, Stats stats) {
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        List<RecipeStepDto> list = new ArrayList<>();
        for (DishTemplateExtractResult.StepDraft item : raw) {
            if (list.size() >= MAX_STEPS) {
                break;
            }
            if (item == null) {
                continue;
            }
            String description = truncate(clean(item.getDescription()), MAX_STEP_DESCRIPTION, stats);
            if (description == null) {
                stats.droppedSteps++;
                continue;
            }
            int order = list.size() + 1;
            Integer duration = normalizeDuration(item.getDurationSeconds(), stats);
            if (duration == null) {
                stats.missingDurations++;
            }
            String timerType = normalizeTimerType(item.getTimerType(), duration);
            list.add(new RecipeStepDto()
                    .setStep(order)
                    .setOrder(order)
                    .setTitle(truncate(clean(item.getTitle()), MAX_STEP_TITLE, stats))
                    .setDescription(description)
                    .setDurationSeconds(duration)
                    .setTimerType(timerType)
                    .setWarning(truncate(clean(item.getWarning()), MAX_HINT, stats))
                    .setCompletionHint(truncate(clean(item.getCompletionHint()), MAX_HINT, stats))
                    .setRequired(item.getRequired() == null || item.getRequired())
                    .setCanUndo(item.getCanUndo() == null || item.getCanUndo()));
        }
        return list;
    }

    private static Integer normalizeDuration(Integer durationSeconds, Stats stats) {
        if (durationSeconds == null || durationSeconds <= 0) {
            return null;
        }
        if (durationSeconds > MAX_STEP_SECONDS) {
            stats.suspiciousDurations++;
            return null;
        }
        return durationSeconds;
    }

    private static String normalizeTimerType(String timerType, Integer durationSeconds) {
        String value = clean(timerType);
        if (value != null) {
            String lower = value.toLowerCase(Locale.ROOT);
            if (TIMER_TYPES.contains(lower)) {
                return lower;
            }
        }
        return durationSeconds == null ? "none" : "countdown";
    }

    private static Integer sumDurations(List<RecipeStepDto> steps) {
        int total = 0;
        for (RecipeStepDto step : steps) {
            if (step.getDurationSeconds() != null) {
                total += step.getDurationSeconds();
            }
        }
        return total > 0 ? total : null;
    }

    private static String normalizeOverall(String overall, String name, List<RecipeStepDto> steps) {
        String value = normalizeConfidence(overall);
        if (value != null) {
            return value;
        }
        return StringUtils.hasText(name) && !steps.isEmpty() ? "MEDIUM" : "LOW";
    }

    private static String normalizeConfidence(String confidence) {
        String value = clean(confidence);
        if (value == null) {
            return null;
        }
        String upper = value.toUpperCase(Locale.ROOT);
        return CONFIDENCES.contains(upper) ? upper : null;
    }

    /**
     * 收集待确认字段：先收录模型自报的非 HIGH 字段，再补齐关键缺失字段，按路径去重。
     */
    private static Map<String, DishTemplateAiExtractVo.UncertainFieldVo> collectUncertain(
            DishTemplateExtractResult raw,
            String name,
            String description,
            Integer servings,
            String difficulty,
            List<RecipeMaterialDto> ingredients,
            List<RecipeStepDto> steps) {
        Map<String, DishTemplateAiExtractVo.UncertainFieldVo> uncertain = new LinkedHashMap<>();
        if (raw.getFieldConfidence() != null) {
            for (Map.Entry<String, String> entry : raw.getFieldConfidence().entrySet()) {
                String path = clean(entry.getKey());
                String confidence = normalizeConfidence(entry.getValue());
                if (path == null || confidence == null || "HIGH".equals(confidence)) {
                    continue;
                }
                putUncertain(uncertain, path, confidence);
            }
        }
        addMissing(uncertain, "name", name);
        addMissing(uncertain, "description", description);
        addMissing(uncertain, "servings", servings);
        addMissing(uncertain, "difficulty", difficulty);
        if (ingredients.isEmpty()) {
            putUncertain(uncertain, "ingredients", "LOW");
        }
        if (steps.isEmpty()) {
            putUncertain(uncertain, "steps", "LOW");
        }
        return uncertain;
    }

    private static void addMissing(Map<String, DishTemplateAiExtractVo.UncertainFieldVo> uncertain,
                                   String path,
                                   Object value) {
        if (value != null) {
            return;
        }
        putUncertain(uncertain, path, "LOW");
    }

    private static void putUncertain(Map<String, DishTemplateAiExtractVo.UncertainFieldVo> uncertain,
                                     String path,
                                     String confidence) {
        if (uncertain.size() >= MAX_UNCERTAIN_FIELDS || uncertain.containsKey(path)) {
            return;
        }
        uncertain.put(path, new DishTemplateAiExtractVo.UncertainFieldVo()
                .setPath(path)
                .setLabel(labelOf(path))
                .setConfidence(confidence));
    }

    private static List<String> buildHints(Stats stats,
                                           String name,
                                           String description,
                                           List<RecipeMaterialDto> ingredients,
                                           List<RecipeStepDto> steps,
                                           String overallConfidence,
                                           String rejectReason) {
        List<String> hints = new ArrayList<>();
        hints.add(REVIEW_HEAD);
        if (rejectReason != null && !EMPTY_DISH.equals(rejectReason)) {
            hints.add("该素材可能不是菜品：" + rejectReason);
        }
        if (name == null) {
            hints.add("未识别到菜名，请手动填写");
        }
        if (description == null) {
            hints.add("未识别到菜品介绍，可以补充口味或适合场景");
        }
        if (ingredients.isEmpty()) {
            hints.add("未识别到原材料，请手动补充");
        }
        if (steps.isEmpty()) {
            hints.add("未识别到做菜步骤，请手动补充");
        }
        if (stats.missingAmounts > 0) {
            hints.add("有 " + stats.missingAmounts + " 个食材未识别到用量，请按实际补充");
        }
        if (stats.droppedIngredients > 0) {
            hints.add("已忽略 " + stats.droppedIngredients + " 条缺少名称的食材");
        }
        if (stats.droppedSteps > 0) {
            hints.add("已忽略 " + stats.droppedSteps + " 条缺少说明的步骤");
        }
        if (stats.missingDurations > 0) {
            hints.add("有 " + stats.missingDurations + " 个步骤未识别到时长，可补充后启用倒计时");
        }
        if (stats.suspiciousDurations > 0) {
            hints.add("有 " + stats.suspiciousDurations + " 个步骤时长明显异常，已忽略");
        }
        if ("LOW".equals(overallConfidence)) {
            hints.add("识别置信度较低，建议重点核对菜名与步骤");
        }
        if (stats.truncated) {
            hints.add("部分内容过长已截断，请在编辑页确认");
        }
        return hints;
    }

    /**
     * 把字段路径翻译成确认页可展示的中文标签。
     */
    static String labelOf(String path) {
        if (path == null) {
            return "";
        }
        String topLevel = switch (path) {
            case "name" -> "菜名";
            case "description" -> "菜品介绍";
            case "servings" -> "建议份数";
            case "difficulty" -> "难度";
            case "tags" -> "标签";
            case "ingredients" -> "原材料";
            case "steps" -> "步骤";
            default -> null;
        };
        if (topLevel != null) {
            return topLevel;
        }
        Matcher matcher = INDEXED_PATH.matcher(path);
        if (!matcher.matches()) {
            return path;
        }
        int index = Integer.parseInt(matcher.group(2)) + 1;
        String field = matcher.group(3);
        boolean ingredient = "ingredients".equals(matcher.group(1));
        String prefix = ingredient ? "第 " + index + " 个食材" : "第 " + index + " 步";
        if (field == null) {
            return prefix;
        }
        String fieldLabel = ingredient ? ingredientFieldLabel(field) : stepFieldLabel(field);
        return fieldLabel == null ? prefix : prefix + "的" + fieldLabel;
    }

    private static String ingredientFieldLabel(String field) {
        return switch (field) {
            case "name" -> "名称";
            case "amount" -> "用量";
            case "unit" -> "单位";
            case "processing" -> "处理方式";
            case "required" -> "是否必需";
            case "substitute" -> "替代方案";
            case "note" -> "备注";
            default -> null;
        };
    }

    private static String stepFieldLabel(String field) {
        return switch (field) {
            case "title" -> "标题";
            case "description" -> "说明";
            case "durationSeconds" -> "时长";
            case "timerType" -> "计时方式";
            case "warning" -> "风险提示";
            case "completionHint" -> "完成提示";
            case "required" -> "是否必需";
            case "canUndo" -> "是否可撤销";
            default -> null;
        };
    }

    private static String normalizeSourceType(String sourceType) {
        String value = clean(sourceType);
        if (value == null) {
            return "AI_TEXT";
        }
        String upper = value.toUpperCase(Locale.ROOT);
        return Set.of("AI_TEXT", "AI_IMAGE", "AI_URL").contains(upper) ? upper : "AI_TEXT";
    }

    private static String normalizeSourceUrl(String sourceUrl) {
        String value = clean(sourceUrl);
        if (value == null) {
            return null;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://") ? value : null;
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String truncate(String value, int max, Stats stats) {
        if (value == null || value.length() <= max) {
            return value;
        }
        stats.truncated = true;
        return value.substring(0, max);
    }

    /**
     * 规范化过程计数，用于生成核对提示。
     */
    private static final class Stats {
        private int missingAmounts;
        private int missingDurations;
        private int droppedIngredients;
        private int droppedSteps;
        private int suspiciousDurations;
        private boolean truncated;
    }
}
