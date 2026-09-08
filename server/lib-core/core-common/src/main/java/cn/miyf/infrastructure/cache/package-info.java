/**
 * 平台缓存抽象：面向系统内部快速查询的对象 / 列表 / 树形缓存门面。
 * 默认基于 Spring Data Redis + Lettuce 实现，可通过 {@code app.redis.enabled} 降级为 Noop。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
package cn.miyf.infrastructure.cache;
