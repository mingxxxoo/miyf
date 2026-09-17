package cn.miyf.ai.support;

import cn.miyf.kitchen.bean.vo.CategoryVo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * 分类名称模糊匹配：包含匹配优先，其次编辑距离。
 *
 * @author XieMingJie
 * @since 2026-09-17
 * @history 1.00 2026-09-17 XieMingJie Created.
 */
@Component
public class CategoryMatcher {

    /**
     * 匹配结果。
     *
     * @param categoryId   已有分类 ID，无匹配时为 null
     * @param categoryName 规范化名称
     * @param newCategory  是否待建新分类
     */
    public record Match(Long categoryId, String categoryName, boolean newCategory) {
    }

    /**
     * 将 AI 推断分类映射到厨房已有分类。
     *
     * @param aiCategoryName AI 分类名
     * @param existing       厨房已有分类
     * @return 匹配结果
     * @history 1.00 2026-09-17 XieMingJie Created.
     */
    public Match match(String aiCategoryName, List<CategoryVo> existing) {
        if (!StringUtils.hasText(aiCategoryName)) {
            return new Match(null, null, false);
        }
        String target = aiCategoryName.trim();
        CategoryVo exact = null;
        CategoryVo contains = null;
        CategoryVo fuzzy = null;
        int bestDistance = Integer.MAX_VALUE;
        for (CategoryVo category : existing) {
            if (category == null || !StringUtils.hasText(category.getName())) {
                continue;
            }
            String name = category.getName().trim();
            if (name.equalsIgnoreCase(target)) {
                exact = category;
                break;
            }
            String lowerName = name.toLowerCase(Locale.ROOT);
            String lowerTarget = target.toLowerCase(Locale.ROOT);
            if (lowerName.contains(lowerTarget) || lowerTarget.contains(lowerName)) {
                contains = category;
            }
            int distance = levenshtein(lowerName, lowerTarget);
            if (distance < bestDistance && distance <= 2) {
                bestDistance = distance;
                fuzzy = category;
            }
        }
        CategoryVo hit = exact != null ? exact : (contains != null ? contains : fuzzy);
        if (hit != null) {
            return new Match(hit.getId(), hit.getName(), false);
        }
        return new Match(null, target, true);
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }
}
