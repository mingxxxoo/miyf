package cn.miyf.workflow;

import java.util.Map;

/**
 * 启动流程请求。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
public class WorkflowStartCommand {

    /** 流程定义 key（如 kitchen_dish_publish） */
    private String processDefinitionKey;
    /** 业务键（如菜品 ID） */
    private String businessKey;
    /** 流程变量 */
    private Map<String, Object> variables;

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public WorkflowStartCommand setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
        return this;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public WorkflowStartCommand setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
        return this;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public WorkflowStartCommand setVariables(Map<String, Object> variables) {
        this.variables = variables;
        return this;
    }
}
