package cn.miyf.infrastructure.redis;

/**
 * Redis Key 约定。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:13
 */
public final class CacheKeys {

    public static final String PREFIX = "ck:";

    private CacheKeys() {
    }

    /**
     * 用户端启用分类列表。
     *
     * @return key
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public static String categoriesEnabled() {
        return PREFIX + "cat:enabled";
    }

    /**
     * 热门菜品。
     *
     * @param limit 条数
     * @return key
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public static String dishesHot(int limit) {
        return PREFIX + "dish:hot:" + limit;
    }

    /**
     * 推荐菜品。
     *
     * @param limit 条数
     * @return key
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public static String dishesRecommend(int limit) {
        return PREFIX + "dish:rec:" + limit;
    }

    /**
     * 热门/推荐相关 key 前缀（用于 SCAN 失效）。
     *
     * @return 前缀
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public static String dishesBrowsePrefix() {
        return PREFIX + "dish:";
    }

    /**
     * 登录失败计数。
     *
     * @param principal 用户名或 openid/code 标识
     * @return key
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public static String loginFail(String principal) {
        return PREFIX + "login:fail:" + principal;
    }

    /**
     * API 限流。
     *
     * @param identity IP 或用户 ID
     * @return key
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public static String apiRate(String identity) {
        return PREFIX + "rl:api:" + identity;
    }

    /**
     * 评分重算锁。
     *
     * @param dishId 菜品 ID
     * @return key
     * @history 1.00 2026-09-05 09:13 XieMingJie Created.
     */
    public static String lockRating(String dishId) {
        return PREFIX + "lock:rating:" + dishId;
    }
}
