package cn.miyf.kitchen.service;

import cn.miyf.infrastructure.cache.CacheClient;
import cn.miyf.kitchen.constant.KitchenCacheKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 厨房浏览缓存失效入口：分类变更、菜品上下架/评分变化时主动清理。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 */
@Service
@RequiredArgsConstructor
public class KitchenCacheEvictService {

    private final CacheClient cacheClient;

    /**
     * 分类列表变更后失效。
     *
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public void evictCategories() {
        if (!cacheClient.isEnabled()) {
            return;
        }
        cacheClient.evict(KitchenCacheKeys.categoriesEnabled());
    }

    /**
     * 菜品浏览缓存（热门/推荐）失效。
     *
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public void evictDishBrowse() {
        if (!cacheClient.isEnabled()) {
            return;
        }
        cacheClient.evictByPrefix(KitchenCacheKeys.dishesBrowsePrefix());
    }

    /**
     * 分类与菜品浏览一并失效。
     *
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public void evictBrowseAll() {
        evictCategories();
        evictDishBrowse();
    }
}
