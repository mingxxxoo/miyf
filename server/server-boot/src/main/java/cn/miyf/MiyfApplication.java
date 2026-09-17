package cn.miyf;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientAutoConfiguration;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.TimeZone;

/**
 * miyf 后端启动入口。
 * <p>
 * 排除 Spring Boot 默认数据源自动配置，改由 HikariCP 多数据源工厂显式装配；
 * Elasticsearch 由 search-service 按 {@code app.search.enabled} 条件装配，排除默认自动配置。
 * DashScope：排除多模态 Embedding（2.0.0-M1.1 缺类）与 Agent（无 Key 也会强制创建 Bean）。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:35
 */
@SpringBootApplication(
        exclude = {
                DataSourceAutoConfiguration.class,
                ElasticsearchClientAutoConfiguration.class,
                ElasticsearchRestClientAutoConfiguration.class
        },
        excludeName = {
                "com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeMultimodalEmbeddingAutoConfiguration",
                "com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAgentAutoConfiguration"
        }
)
@MapperScan({"cn.miyf.**.repository", "cn.miyf.**.repository.mapper", "cn.miyf.**.mapper"})
@EnableTransactionManagement
public class MiyfApplication {

    /**
     * 应用入口。
     *
     * @param args 启动参数
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(MiyfApplication.class, args);
    }
}
