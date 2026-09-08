package cn.miyf.search.config;

import cn.miyf.config.SearchConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 搜索模块配置入口。
 * 通过 {@link Import} 引入 {@link SearchConfig}，便于按模块边界扫描装配。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
@Configuration
@Import(SearchConfig.class)
public class SearchModuleConfig {
}
