package cn.miyf.workflow.internal;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.workflow.WorkflowDecision;
import cn.miyf.workflow.WorkflowEngine;
import cn.miyf.workflow.WorkflowStartCommand;
import cn.miyf.workflow.WorkflowTask;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Flowable 实现的公共审批引擎。
 * 对外仅暴露 {@link WorkflowEngine}，屏蔽 Flowable API。
 *
 * @author XieMingJie
 * @since 2026-09-15
 * @history 1.00 2026-09-15 XieMingJie Created.
 */
@Service
public class FlowableWorkflowEngine implements WorkflowEngine {

    /** BPMN 网关用：是否通过 */
    public static final String VAR_APPROVED = "approved";
    /** BPMN 变量：驳回原因 */
    public static final String VAR_REJECT_REASON = "rejectReason";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final HistoryService historyService;

    public FlowableWorkflowEngine(RuntimeService runtimeService,
                                  TaskService taskService,
                                  HistoryService historyService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public String start(WorkflowStartCommand command) {
        if (command == null || !StringUtils.hasText(command.getProcessDefinitionKey())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "流程定义不能为空");
        }
        Map<String, Object> vars = command.getVariables() == null
                ? new HashMap<>()
                : new HashMap<>(command.getVariables());
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                command.getProcessDefinitionKey(),
                command.getBusinessKey(),
                vars);
        return instance.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WorkflowTask> listCandidateGroupTasks(String candidateGroup, int firstResult, int maxResults) {
        if (!StringUtils.hasText(candidateGroup)) {
            return List.of();
        }
        int first = Math.max(firstResult, 0);
        // 单页上限 100，避免一次拉全量待办
        int max = Math.min(Math.max(maxResults, 1), 100);
        return taskService.createTaskQuery()
                .taskCandidateGroup(candidateGroup)
                .active()
                .orderByTaskCreateTime()
                .desc()
                .listPage(first, max)
                .stream()
                .map(this::toTask)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowTask requireTask(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "审核任务不存在");
        }
        return toTask(task);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void complete(String taskId, WorkflowDecision decision, String rejectReason) {
        requireTask(taskId);
        if (decision == WorkflowDecision.REJECTED && !StringUtils.hasText(rejectReason)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "驳回原因不能为空");
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put(VAR_APPROVED, decision == WorkflowDecision.APPROVED);
        vars.put(VAR_REJECT_REASON, rejectReason == null ? "" : rejectReason.trim());
        taskService.complete(taskId, vars);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteProcess(String processInstanceId, String reason) {
        if (!StringUtils.hasText(processInstanceId)) {
            return;
        }
        ProcessInstance running = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        // 已结束实例无需再删，避免 Flowable 抛错
        if (running != null) {
            runtimeService.deleteProcessInstance(processInstanceId, reason);
        }
    }

    private WorkflowTask toTask(Task task) {
        String processKey = null;
        if (StringUtils.hasText(task.getProcessDefinitionId())) {
            // Flowable 定义 ID 形如 key:version:uuid，截取 key
            int colon = task.getProcessDefinitionId().indexOf(':');
            processKey = colon > 0
                    ? task.getProcessDefinitionId().substring(0, colon)
                    : task.getProcessDefinitionId();
        }
        String businessKey = null;
        if (StringUtils.hasText(task.getProcessInstanceId())) {
            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            if (instance != null) {
                businessKey = instance.getBusinessKey();
            } else {
                // 运行时已结束时从历史取 businessKey
                var historic = historyService.createHistoricProcessInstanceQuery()
                        .processInstanceId(task.getProcessInstanceId())
                        .singleResult();
                if (historic != null) {
                    businessKey = historic.getBusinessKey();
                }
            }
        }
        Map<String, Object> variables = taskService.getVariables(task.getId());
        return new WorkflowTask()
                .setTaskId(task.getId())
                .setProcessInstanceId(task.getProcessInstanceId())
                .setProcessDefinitionKey(processKey)
                .setBusinessKey(businessKey)
                .setName(task.getName())
                .setVariables(variables);
    }
}
