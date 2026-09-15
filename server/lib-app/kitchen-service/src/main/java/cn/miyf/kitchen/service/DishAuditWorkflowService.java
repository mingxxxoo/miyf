package cn.miyf.kitchen.service;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.kitchen.bean.entity.DishEntity;
import cn.miyf.kitchen.bean.entity.KitchenEntity;
import cn.miyf.kitchen.bean.vo.DishAuditTaskVo;
import cn.miyf.kitchen.repository.DishRepository;
import cn.miyf.kitchen.repository.KitchenRepository;
import cn.miyf.workflow.WorkflowDecision;
import cn.miyf.workflow.WorkflowEngine;
import cn.miyf.workflow.WorkflowStartCommand;
import cn.miyf.workflow.WorkflowTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜品审核：启动公共 Flowable 流程，回写业务审核状态。不含价格字段。
 * <p>
 * BPMN 候选组 {@link #CANDIDATE_GROUP} 用于 TaskQuery；管理端待办与通过/驳回
 * 另由 IAM 权限 {@code kitchen:dish:audit} 鉴权，未强制同步 Flowable Identity。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
@Service
@RequiredArgsConstructor
public class DishAuditWorkflowService {

    public static final String PROCESS_KEY = "kitchen_dish_publish";
    public static final String CANDIDATE_GROUP = "kitchen_dish_auditor";

    private final WorkflowEngine workflowEngine;
    private final DishRepository dishRepository;
    private final KitchenRepository kitchenRepository;

    /**
     * 提交审核：清理旧实例后启流，状态置 PENDING_REVIEW；上架中则先下架。
     *
     * @param dish 已落库菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public void submit(DishEntity dish) {
        if (dish == null || dish.getId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "菜品无效");
        }
        if ("PENDING_REVIEW".equals(dish.getAuditStatus()) && StringUtils.hasText(dish.getProcessInstanceId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "菜品正在审核中，请先撤回或等待结果");
        }
        if (StringUtils.hasText(dish.getProcessInstanceId())) {
            // 驳回/草稿后重提：删除残留流程实例，避免双实例
            workflowEngine.deleteProcess(dish.getProcessInstanceId(), "resubmit");
        }
        Map<String, Object> vars = new HashMap<>();
        vars.put("dishId", dish.getId());
        vars.put("kitchenId", dish.getKitchenId());
        vars.put("dishName", dish.getName());
        String instanceId = workflowEngine.start(new WorkflowStartCommand()
                .setProcessDefinitionKey(PROCESS_KEY)
                .setBusinessKey(String.valueOf(dish.getId()))
                .setVariables(vars));
        dish.setAuditStatus("PENDING_REVIEW");
        dish.setProcessInstanceId(instanceId);
        dish.setRejectReason(null);
        if ("ON_SALE".equals(dish.getStatus())) {
            // 审核期间不可售，防止未审内容继续对外
            dish.setStatus("OFF_SALE");
        }
        dishRepository.updateById(dish);
    }

    /**
     * 管理端待办列表（候选组 {@link #CANDIDATE_GROUP}）。
     *
     * @param first 偏移
     * @param max   条数上限
     * @return 待办 VO
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public List<DishAuditTaskVo> listPending(int first, int max) {
        return workflowEngine.listCandidateGroupTasks(CANDIDATE_GROUP, first, max).stream()
                .map(this::toVo)
                .toList();
    }

    /**
     * 通过审核任务，回写 APPROVED。
     *
     * @param taskId Flowable 任务 ID
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public void approve(String taskId) {
        complete(taskId, WorkflowDecision.APPROVED, null);
    }

    /**
     * 驳回审核任务，回写 REJECTED 并下架。
     *
     * @param taskId Flowable 任务 ID
     * @param reason 驳回原因
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public void reject(String taskId, String reason) {
        complete(taskId, WorkflowDecision.REJECTED, reason);
    }

    /**
     * 撤回审核中的流程，菜品回退为草稿。
     *
     * @param dish 审核中菜品
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public void withdraw(DishEntity dish) {
        if (dish == null || dish.getId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "菜品无效");
        }
        if (!"PENDING_REVIEW".equals(dish.getAuditStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "仅审核中可撤回");
        }
        if (StringUtils.hasText(dish.getProcessInstanceId())) {
            workflowEngine.deleteProcess(dish.getProcessInstanceId(), "withdraw");
        }
        dish.setAuditStatus("DRAFT");
        dish.setProcessInstanceId(null);
        dish.setRejectReason(null);
        dishRepository.updateById(dish);
    }

    private void complete(String taskId, WorkflowDecision decision, String reason) {
        WorkflowTask task = workflowEngine.requireTask(taskId);
        Long dishId = parseDishId(task);
        DishEntity dish = dishRepository.selectById(dishId);
        if (dish == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜品不存在");
        }
        workflowEngine.complete(taskId, decision, reason);
        if (decision == WorkflowDecision.APPROVED) {
            dish.setAuditStatus("APPROVED");
            dish.setRejectReason(null);
        } else {
            dish.setAuditStatus("REJECTED");
            dish.setRejectReason(reason);
            dish.setStatus("OFF_SALE");
        }
        dishRepository.updateById(dish);
    }

    private Long parseDishId(WorkflowTask task) {
        Object var = task.getVariables() == null ? null : task.getVariables().get("dishId");
        if (var instanceof Number number) {
            return number.longValue();
        }
        if (var != null && StringUtils.hasText(var.toString())) {
            return Long.valueOf(var.toString());
        }
        if (StringUtils.hasText(task.getBusinessKey())) {
            return Long.valueOf(task.getBusinessKey());
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "审核任务缺少菜品信息");
    }

    private DishAuditTaskVo toVo(WorkflowTask task) {
        Long dishId = parseDishId(task);
        DishEntity dish = dishRepository.selectById(dishId);
        KitchenEntity kitchen = dish == null || dish.getKitchenId() == null
                ? null
                : kitchenRepository.selectById(dish.getKitchenId());
        return new DishAuditTaskVo()
                .setTaskId(task.getTaskId())
                .setDishId(dishId)
                .setKitchenId(dish == null ? null : dish.getKitchenId())
                .setDishName(dish == null ? null : dish.getName())
                .setKitchenName(kitchen == null ? null : kitchen.getName())
                .setProcessInstanceId(task.getProcessInstanceId());
    }
}
