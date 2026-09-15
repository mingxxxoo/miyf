package cn.miyf.workflow.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 审批模块自动装配：扫描 Flowable 封装实现。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
@AutoConfiguration
@ComponentScan(basePackages = "cn.miyf.workflow")
public class WorkflowAutoConfiguration {
}
