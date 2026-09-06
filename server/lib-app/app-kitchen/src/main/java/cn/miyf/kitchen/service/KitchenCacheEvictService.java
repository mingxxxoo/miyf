package cn.miyf.kitchen.service;

import cn.miyf.config.RedisAppProperties;
import cn.miyf.kitchen.constant.CacheKeys;
import cn.miyf.infrastructure.redis.RedisJsonCache;
import org.springframework.stereotype.Service;

/**
 * 厨房浏览缓存失效入口：分类变更、菜品上下架/评分变化时主动清理。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 */
@Service
public class KitchenCacheEvictService {

    private final RedisJsonCache redisJsonCache;
    private final RedisAppProperties properties;

    /**
     * 构造失效服务。
     *
     * @param redisJsonCache 缓存
     * @param properties     配置
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public KitchenCacheEvictService(RedisJsonCache redisJsonCache, RedisAppProperties properties) {
        this.redisJsonCache = redisJsonCache;
        this.properties = properties;
    }

    /**
     * 分类列表变更后失效。
     *
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public void evictCategories() {
        if (!properties.isEnabled()) {
            return;
        }
        redisJsonCache.delete(CacheKeys.categoriesEnabled());
    }

    /**
     * 菜品浏览缓存（热门/推荐）失效。
     *
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public void evictDishBrowse() {
        if (!properties.isEnabled()) {
            return;
        }
        redisJsonCache.deleteByPrefix(CacheKeys.dishesBrowsePrefix());
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
