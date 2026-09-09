/**
 * 平台缓存抽象：面向系统内部快速查询的对象 / 列表 / 树形缓存门面。
 * <p>
 * Key <b>规范</b>见 {@link cn.miyf.infrastructure.cache.CacheKeys}；具体 key 由各业务模块定义
 * （如 KitchenCacheKeys / OssCacheKeys / AuthCacheKeys）。
 * 默认基于 Spring Data Redis + Lettuce 实现，可通过 {@code app.redis.enabled} 降级为 Noop。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
package cn.miyf.infrastructure.cache;
