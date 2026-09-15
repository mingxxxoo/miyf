package cn.miyf.workflow;

import java.util.List;

/**
 * 流程引擎门面：启流、查待办、完成、撤销。业务模块不直接依赖 Flowable API。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
public interface WorkflowEngine {

    /**
     * 启动流程，返回流程实例 ID。
     *
     * @param command 流程定义 key、业务键与变量
     * @return 流程实例 ID
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    String start(WorkflowStartCommand command);

    /**
     * 按候选组查询待办。
     *
     * @param candidateGroup 候选组 ID
     * @param firstResult    偏移
     * @param maxResults     条数上限
     * @return 待办列表
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    List<WorkflowTask> listCandidateGroupTasks(String candidateGroup, int firstResult, int maxResults);

    /**
     * 按任务 ID 查询；不存在则抛业务异常。
     *
     * @param taskId 任务 ID
     * @return 待办任务
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    WorkflowTask requireTask(String taskId);

    /**
     * 完成审核任务。
     *
     * @param taskId       任务 ID
     * @param decision     通过或驳回
     * @param rejectReason 驳回原因（驳回时必填）
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    void complete(String taskId, WorkflowDecision decision, String rejectReason);

    /**
     * 删除未结束流程（撤回重提）。
     *
     * @param processInstanceId 流程实例 ID
     * @param reason            删除原因
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    void deleteProcess(String processInstanceId, String reason);
}
