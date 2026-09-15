package cn.miyf.workflow;

import java.util.Map;

/**
 * 流程待办任务。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
public class WorkflowTask {

    /** 任务 ID */
    private String taskId;
    /** 流程实例 ID */
    private String processInstanceId;
    /** 流程定义 key */
    private String processDefinitionKey;
    /** 业务键 */
    private String businessKey;
    /** 任务名称 */
    private String name;
    /** 流程变量快照 */
    private Map<String, Object> variables;

    public String getTaskId() {
        return taskId;
    }

    public WorkflowTask setTaskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public WorkflowTask setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public WorkflowTask setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
        return this;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public WorkflowTask setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
        return this;
    }

    public String getName() {
        return name;
    }

    public WorkflowTask setName(String name) {
        this.name = name;
        return this;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public WorkflowTask setVariables(Map<String, Object> variables) {
        this.variables = variables;
        return this;
    }
}
