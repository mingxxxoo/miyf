package cn.miyf;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * miyf 后端启动入口。
 * <p>
 * 排除 Spring Boot 默认数据源自动配置，改由 Druid 多数据源工厂显式装配。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:35
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@MapperScan({"cn.miyf.**.repository.mapper", "cn.miyf.**.mapper"})
@EnableTransactionManagement
public class MiyfApplication {

    /**
     * 应用入口。
     *
     * @param args 启动参数
     * @history 1.00 2026-09-04 16:35 XieMingJie Created.
     */
    public static void main(String[] args) {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(MiyfApplication.class, args);
    }
}
