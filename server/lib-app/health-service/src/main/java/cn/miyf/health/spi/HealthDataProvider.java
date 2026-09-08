package cn.miyf.health.spi;

import java.util.List;
import java.util.Set;

/**
 * 健康数据源 SPI：厂商/开放平台实现此接口并注册为 Spring Bean 即可接入。
 * <p>
 * 约定：实现只负责拉取并映射为 {@link HealthSampleDraft}（canonical metric），
 * 不去写库；入库、去重、同步任务由应用服务统一处理。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
public interface HealthDataProvider {

    /**
     * 稳定编码，如 {@code manual}、{@code example}。
     *
     * @return 编码
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    String code();

    /**
     * 展示名。
     *
     * @return 名称
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    String displayName();

    /**
     * 是否启用。
     *
     * @return true 可用
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    boolean enabled();

    /**
     * 是否支持远程拉取（手动录入类返回 false）。
     *
     * @return true 可 sync
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    default boolean supportsRemoteFetch() {
        return true;
    }

    /**
     * 支持的规范指标编码集合。
     *
     * @return 指标
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    Set<String> supportedMetrics();

    /**
     * 拉取远程采样草稿。
     *
     * @param request 请求
     * @return 草稿列表，不可为 null
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    List<HealthSampleDraft> fetch(HealthFetchRequest request);
}
