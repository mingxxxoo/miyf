package cn.miyf.infrastructure.cache;

/**
 * 单对象缓存：适合详情、配置项、用户会话附属等单体 JSON。
 *
 * @param <T> 对象类型
 * @author XieMingJie
 * @since 2026-09-08
 */
public interface ObjectCache<T> extends ValueCache<T> {
}
