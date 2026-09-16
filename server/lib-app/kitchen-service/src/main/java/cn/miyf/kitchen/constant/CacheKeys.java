package cn.miyf.kitchen.constant;

/**
 * @deprecated 使用 {@link KitchenCacheKeys}
 */
@Deprecated
public final class CacheKeys {

    private CacheKeys() {
    }

    /** @deprecated 使用 {@link KitchenCacheKeys#categoriesEnabled(Long)} */
    @Deprecated
    public static String categoriesEnabled() {
        return KitchenCacheKeys.categoriesEnabledPrefix();
    }

    /** @deprecated 使用 {@link KitchenCacheKeys#dishesHot(int)} */
    @Deprecated
    public static String dishesHot(int limit) {
        return KitchenCacheKeys.dishesHot(limit);
    }

    /** @deprecated 使用 {@link KitchenCacheKeys#dishesRecommend(int)} */
    @Deprecated
    public static String dishesRecommend(int limit) {
        return KitchenCacheKeys.dishesRecommend(limit);
    }

    /** @deprecated 使用 {@link KitchenCacheKeys#dishesBrowsePrefix()} */
    @Deprecated
    public static String dishesBrowsePrefix() {
        return KitchenCacheKeys.dishesBrowsePrefix();
    }

    /** @deprecated 使用 {@link KitchenCacheKeys#lockRating(String)} */
    @Deprecated
    public static String lockRating(String dishId) {
        return KitchenCacheKeys.lockRating(dishId);
    }
}
