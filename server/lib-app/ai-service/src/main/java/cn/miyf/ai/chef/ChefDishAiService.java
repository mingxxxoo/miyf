package cn.miyf.ai.chef;

import cn.miyf.ai.bean.dto.ChefDishAiExtractDto;
import cn.miyf.ai.bean.model.ChefDishExtractResult;
import cn.miyf.ai.bean.vo.ChefDishAiExtractVo;
import cn.miyf.ai.config.AiProperties;
import cn.miyf.ai.prompt.AiPromptTemplates;
import cn.miyf.ai.support.AiStructuredCaller;
import cn.miyf.ai.support.CategoryMatcher;
import cn.miyf.ai.support.WebPageFetcher;
import cn.miyf.auth.security.SecurityUtils;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.kitchen.bean.vo.CategoryVo;
import cn.miyf.kitchen.service.CategoryApplicationService;
import cn.miyf.kitchen.service.KitchenAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 场景一：文本/网页链接 → 菜品草稿（不落库，厨师确认后走现有提审流程）。
 *
 * @author XieMingJie
 * @since 2026-09-17
 * @history 1.00 2026-09-17 XieMingJie Created.
 */
@Service
@RequiredArgsConstructor
public class ChefDishAiService {

    private final AiStructuredCaller aiStructuredCaller;
    private final AiProperties aiProperties;
    private final WebPageFetcher webPageFetcher;
    private final CategoryApplicationService categoryApplicationService;
    private final CategoryMatcher categoryMatcher;
    private final KitchenAccessService kitchenAccessService;

    /**
     * 抽取菜品草稿。
     *
     * @param dto 文本或链接
     * @return 确认页 VO
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    public ChefDishAiExtractVo extract(ChefDishAiExtractDto dto) {
        Long kitchenId = kitchenAccessService.requireOwnedKitchen().getId();
        List<CategoryVo> categories = categoryApplicationService.listChef();
        String categoryNames = categories.stream()
                .map(CategoryVo::getName)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("、"));

        String userPrompt;
        String inputSummary;
        if (dto != null && StringUtils.hasText(dto.getText())) {
            userPrompt = AiPromptTemplates.chefTextUser(dto.getText().trim(), categoryNames);
            inputSummary = summarize(dto.getText().trim());
        } else if (dto != null && StringUtils.hasText(dto.getUrl())) {
            // 抓取失败直接抛业务异常，不进入 AI
            WebPageFetcher.FetchedPage page = webPageFetcher.fetch(dto.getUrl().trim());
            userPrompt = AiPromptTemplates.chefUrlUser(page.title(), page.finalUrl(), page.text(), categoryNames);
            inputSummary = "url:" + page.finalUrl();
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请提供菜品文本或网页链接");
        }

        if (!aiStructuredCaller.isAvailable()) {
            return blankForm("AI 暂不可用，请手动填写菜品");
        }

        try {
            ChefDishExtractResult result = aiStructuredCaller.callEntity(
                    "CHEF_DISH",
                    AiPromptTemplates.CHEF_DISH_SYSTEM,
                    userPrompt,
                    aiProperties.getChefDish().getTemperature(),
                    aiProperties.getChefDish().getRetryOnTransient(),
                    ChefDishExtractResult.class,
                    SecurityUtils.currentUserId(),
                    kitchenId,
                    inputSummary);
            return toVo(result, categories);
        } catch (Exception ex) {
            return blankForm("AI 抽取失败，请手动填写菜品");
        }
    }

    private ChefDishAiExtractVo blankForm(String reason) {
        return new ChefDishAiExtractVo()
                .setDegraded(true)
                .setRejectReason(reason)
                .setDishes(List.of(emptyDish()));
    }

    private ChefDishAiExtractVo.DishDraftVo emptyDish() {
        return new ChefDishAiExtractVo.DishDraftVo()
                .setName("")
                .setSubtitle(null)
                .setCategoryName(null)
                .setCategoryId(null)
                .setNewCategory(false)
                .setDescription("")
                .setTags(new ArrayList<>())
                .setStockType("LIMITED")
                .setStock(10)
                .setRecipe(new ChefDishAiExtractVo.RecipeDraftVo())
                .setNutrition(null)
                .setAiTruncated(false);
    }

    private ChefDishAiExtractVo toVo(ChefDishExtractResult result, List<CategoryVo> categories) {
        ChefDishAiExtractVo vo = new ChefDishAiExtractVo().setDegraded(false);
        if (result == null) {
            return blankForm("未识别到菜品信息");
        }
        if (result.getDishes() == null || result.getDishes().isEmpty()) {
            vo.setRejectReason(StringUtils.hasText(result.getRejectReason())
                    ? result.getRejectReason() : "未识别到菜品信息");
            vo.setDishes(List.of());
            return vo;
        }
        List<ChefDishAiExtractVo.DishDraftVo> dishes = new ArrayList<>();
        for (ChefDishExtractResult.DishDraft draft : result.getDishes()) {
            dishes.add(postProcess(draft, categories));
        }
        vo.setDishes(dishes);
        vo.setRejectReason(result.getRejectReason());
        return vo;
    }

    private ChefDishAiExtractVo.DishDraftVo postProcess(ChefDishExtractResult.DishDraft draft,
                                                        List<CategoryVo> categories) {
        ChefDishAiExtractVo.DishDraftVo vo = new ChefDishAiExtractVo.DishDraftVo();
        boolean truncated = false;
        String name = draft.getName() == null ? "" : draft.getName().trim();
        if (name.length() > 12) {
            name = name.substring(0, 12);
            truncated = true;
        }
        String description = draft.getDescription();
        if (!StringUtils.hasText(description)) {
            description = StringUtils.hasText(draft.getSubtitle())
                    ? draft.getSubtitle()
                    : (StringUtils.hasText(name) ? name + "，待厨师补充简介" : "待厨师补充简介");
        } else if (description.length() > 80) {
            description = description.substring(0, 80);
            truncated = true;
        }
        CategoryMatcher.Match match = categoryMatcher.match(draft.getCategoryName(), categories);
        String stockType = normalizeStockType(draft.getStockType());
        Integer stock = draft.getStock() == null ? 10 : draft.getStock();
        if ("UNLIMITED".equals(stockType)) {
            stock = null;
        }
        vo.setName(name)
                .setSubtitle(draft.getSubtitle())
                .setCategoryName(match.categoryName())
                .setCategoryId(match.categoryId())
                .setNewCategory(match.newCategory())
                .setDescription(description)
                .setTags(draft.getTags() == null ? List.of() : draft.getTags())
                .setStockType(stockType)
                .setStock(stock)
                .setRecipe(mapRecipe(draft.getRecipe()))
                .setNutrition(draft.getNutrition())
                .setAiTruncated(truncated);
        return vo;
    }

    private ChefDishAiExtractVo.RecipeDraftVo mapRecipe(ChefDishExtractResult.RecipeDraft recipe) {
        ChefDishAiExtractVo.RecipeDraftVo vo = new ChefDishAiExtractVo.RecipeDraftVo();
        if (recipe == null) {
            return vo;
        }
        if (recipe.getIngredients() != null) {
            vo.setIngredients(recipe.getIngredients().stream()
                    .map(i -> new ChefDishAiExtractVo.NamedAmountVo(i.getName(), i.getAmount()))
                    .toList());
        }
        if (recipe.getSeasonings() != null) {
            vo.setSeasonings(recipe.getSeasonings().stream()
                    .map(i -> new ChefDishAiExtractVo.NamedAmountVo(i.getName(), i.getAmount()))
                    .toList());
        }
        if (recipe.getSteps() != null) {
            vo.setSteps(recipe.getSteps().stream()
                    .map(s -> new ChefDishAiExtractVo.StepDraftVo(s.getStep(), s.getContent(), s.getDurationMinutes()))
                    .toList());
        }
        vo.setDifficulty(recipe.getDifficulty())
                .setPrepareMinutes(recipe.getPrepareMinutes())
                .setCookMinutes(recipe.getCookMinutes())
                .setServings(recipe.getServings());
        return vo;
    }

    private String normalizeStockType(String stockType) {
        if (!StringUtils.hasText(stockType)) {
            return "LIMITED";
        }
        String upper = stockType.trim().toUpperCase(Locale.ROOT);
        return "UNLIMITED".equals(upper) ? "UNLIMITED" : "LIMITED";
    }

    private String summarize(String text) {
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 120 ? oneLine.substring(0, 120) : oneLine;
    }
}
