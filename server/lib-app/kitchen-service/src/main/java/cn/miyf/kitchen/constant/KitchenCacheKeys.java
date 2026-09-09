package cn.miyf.kitchen.constant;

import cn.miyf.infrastructure.cache.CacheKeys;

/**
 * 厨房模块缓存 Key（遵循 {@link CacheKeys} 规范）。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 */
public final class KitchenCacheKeys {

    private KitchenCacheKeys() {
    }

    /**
     * 用户端启用分类列表。
     *
     * @return key
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public static String categoriesEnabled() {
        return CacheKeys.join(CacheKeys.DOMAIN_DATA, "cat", "enabled");
    }

    /**
     * 热门菜品。
     *
     * @param limit 条数
     * @return key
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public static String dishesHot(int limit) {
        return CacheKeys.join(CacheKeys.DOMAIN_DATA, "dish", "hot", limit);
    }

    /**
     * 推荐菜品。
     *
     * @param limit 条数
     * @return key
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public static String dishesRecommend(int limit) {
        return CacheKeys.join(CacheKeys.DOMAIN_DATA, "dish", "rec", limit);
    }

    /**
     * 热门/推荐相关 key 前缀（SCAN 失效）。
     *
     * @return 前缀
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public static String dishesBrowsePrefix() {
        return CacheKeys.join(CacheKeys.DOMAIN_DATA, "dish") + ":";
    }

    /**
     * 评分重算锁。
     *
     * @param dishId 菜品 ID
     * @return key
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public static String lockRating(String dishId) {
        return CacheKeys.join(CacheKeys.DOMAIN_LOCK, "rating", dishId);
    }
}
