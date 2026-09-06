package cn.miyf.common;

import java.util.HashMap;
import java.util.Map;

/**
 * 基于 ThreadLocal 的线程级键值上下文。
 * <p>
 * 用于在同一请求线程内传递登录用户等信息；务必在请求结束时 {@link #clear()}，
 * 避免容器线程池复用导致数据泄漏。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:25
 */
public final class LocalThreadMap {

    private static final ThreadLocal<Map<String, Object>> CONTEXT = ThreadLocal.withInitial(HashMap::new);

    private LocalThreadMap() {
    }

    /**
     * 写入键值。
     *
     * @param key   键
     * @param value 值，允许为 null（等同于移除）
     * @history 1.00 2026-09-04 17:25 XieMingJie Created.
     */
    public static void put(String key, Object value) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("LocalThreadMap key must not be blank");
        }
        if (value == null) {
            CONTEXT.get().remove(key);
            return;
        }
        CONTEXT.get().put(key, value);
    }

    /**
     * 读取键对应的值并做类型转换。
     *
     * @param key  键
     * @param type 期望类型
     * @param <T>  返回类型
     * @return 值；不存在或类型不匹配时返回 null
     * @history 1.00 2026-09-04 17:25 XieMingJie Created.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key, Class<T> type) {
        if (key == null || type == null) {
            return null;
        }
        Object value = CONTEXT.get().get(key);
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            return null;
        }
        return (T) value;
    }

    /**
     * 读取原始值。
     *
     * @param key 键
     * @return 值，不存在时为 null
     * @history 1.00 2026-09-04 17:25 XieMingJie Created.
     */
    public static Object get(String key) {
        if (key == null) {
            return null;
        }
        return CONTEXT.get().get(key);
    }

    /**
     * 是否包含指定键。
     *
     * @param key 键
     * @return true 表示存在
     * @history 1.00 2026-09-04 17:25 XieMingJie Created.
     */
    public static boolean contains(String key) {
        return key != null && CONTEXT.get().containsKey(key);
    }

    /**
     * 移除指定键。
     *
     * @param key 键
     * @history 1.00 2026-09-04 17:25 XieMingJie Created.
     */
    public static void remove(String key) {
        if (key == null) {
            return;
        }
        CONTEXT.get().remove(key);
    }

    /**
     * 清空当前线程全部上下文，防止线程复用泄漏。
     *
     * @history 1.00 2026-09-04 17:25 XieMingJie Created.
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
