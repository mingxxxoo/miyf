/**
 * 健康数据源 SPI：实现 {@link cn.miyf.health.spi.HealthDataProvider} 并注册为 Spring Bean 即可接入。
 * <p>
 * 实现只负责拉取并映射为规范 {@link cn.miyf.health.spi.HealthSampleDraft}；
 * 入库、幂等、同步任务由 {@link cn.miyf.health.service.HealthApplicationService} 统一处理。
 */
package cn.miyf.health.spi;
