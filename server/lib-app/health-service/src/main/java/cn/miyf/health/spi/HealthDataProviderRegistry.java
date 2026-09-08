package cn.miyf.health.spi;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 健康数据源注册表：聚合全部 {@link HealthDataProvider} Bean。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Component
public class HealthDataProviderRegistry {

    private final Map<String, HealthDataProvider> providers = new LinkedHashMap<>();

    /**
     * 聚合全部 Provider Bean；编码重复则启动失败。
     *
     * @param providerList Spring 注入列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public HealthDataProviderRegistry(List<HealthDataProvider> providerList) {
        if (providerList != null) {
            for (HealthDataProvider provider : providerList) {
                if (provider == null || provider.code() == null || provider.code().isBlank()) {
                    continue;
                }
                String code = provider.code().trim();
                if (providers.containsKey(code)) {
                    throw new IllegalStateException("Duplicate health provider code: " + code);
                }
                providers.put(code, provider);
            }
        }
    }

    /**
     * 全部数据源（含未启用）。
     *
     * @return 集合
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public Collection<HealthDataProvider> all() {
        return List.copyOf(providers.values());
    }

    /**
     * 已启用数据源。
     *
     * @return 列表
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public List<HealthDataProvider> enabled() {
        return providers.values().stream().filter(HealthDataProvider::enabled).toList();
    }

    /**
     * 按编码查找。
     *
     * @param code 编码
     * @return Optional
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public Optional<HealthDataProvider> find(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(providers.get(code.trim()));
    }

    /**
     * 要求存在且启用。
     *
     * @param code 编码
     * @return 提供者
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public HealthDataProvider requireEnabled(String code) {
        HealthDataProvider provider = find(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "健康数据源不存在: " + code));
        if (!provider.enabled()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "健康数据源未启用: " + code);
        }
        return provider;
    }
}
