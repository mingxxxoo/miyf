package cn.miyf.service;

import cn.miyf.bean.entity.SysConfigEntity;
import cn.miyf.repository.mapper.SysConfigMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置只读门面：带本地缓存，供业务开关与参数热读取（如搜索召回、文件签名 TTL）。
 *
 * @author XieMingJie
 * @since 2026-09-08
 * @history 1.00 2026-09-08 XieMingJie Created.
 */
@Service
@RequiredArgsConstructor
public class SystemConfigReader {

    private final SysConfigMapper configMapper;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /**
     * 读取布尔配置；键不存在或非 ENABLED 时返回默认值。
     *
     * @param configKey    配置键
     * @param defaultValue 默认值
     * @return 布尔值
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public boolean getBoolean(String configKey, boolean defaultValue) {
        String raw = getRaw(configKey);
        if (!StringUtils.hasText(raw)) {
            return defaultValue;
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(v) || "1".equals(v) || "yes".equals(v) || "on".equals(v)) {
            return true;
        }
        if ("false".equals(v) || "0".equals(v) || "no".equals(v) || "off".equals(v)) {
            return false;
        }
        return defaultValue;
    }

    /**
     * 读取长整型数字配置；键不存在、非 ENABLED 或无法解析时返回默认值。
     *
     * @param configKey    配置键
     * @param defaultValue 默认值
     * @return 数值
     * @history 1.00 2026-09-10 XieMingJie Created.
     */
    public long getLong(String configKey, long defaultValue) {
        String raw = getRaw(configKey);
        if (!StringUtils.hasText(raw)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * 读取原始字符串值（仅 ENABLED 配置）。
     *
     * @param configKey 配置键
     * @return 值，不存在则为 null
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public String getRaw(String configKey) {
        if (!StringUtils.hasText(configKey)) {
            return null;
        }
        String key = configKey.trim();
        if (cache.containsKey(key)) {
            String cached = cache.get(key);
            return StringUtils.hasText(cached) ? cached : null;
        }
        SysConfigEntity entity = configMapper.selectOne(Wrappers.<SysConfigEntity>lambdaQuery()
                .eq(SysConfigEntity::getConfigKey, key)
                .eq(SysConfigEntity::getStatus, "ENABLED")
                .last("LIMIT 1"));
        String value = entity == null ? null : entity.getConfigValue();
        // null 也缓存，避免反复打库；更新时 invalidate
        cache.put(key, value == null ? "" : value);
        return StringUtils.hasText(value) ? value : null;
    }

    /**
     * 失效单个键缓存。
     *
     * @param configKey 配置键
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public void invalidate(String configKey) {
        if (StringUtils.hasText(configKey)) {
            cache.remove(configKey.trim());
        }
    }

    /**
     * 清空全部缓存。
     *
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public void invalidateAll() {
        cache.clear();
    }
}
