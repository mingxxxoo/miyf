/**
 * Elasticsearch 搜索横向能力。
 * 对外通过 {@link cn.miyf.infrastructure.search.SearchClient} 提供索引维护与查询封装，
 * 默认可由 Noop 实现降级，业务模块按需依赖本包。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
package cn.miyf.search;
