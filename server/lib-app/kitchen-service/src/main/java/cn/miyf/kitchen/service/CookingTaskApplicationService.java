package cn.miyf.kitchen.service;

import cn.miyf.auth.security.AuthPrincipal;
import cn.miyf.auth.security.SecurityUtils;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.dto.CookingTaskCreateDto;
import cn.miyf.kitchen.bean.entity.CookingTaskEntity;
import cn.miyf.kitchen.bean.entity.CookingTaskStepEntity;
import cn.miyf.kitchen.bean.entity.DishEntity;
import cn.miyf.kitchen.bean.entity.KitchenEntity;
import cn.miyf.kitchen.bean.entity.OperationLogEntity;
import cn.miyf.kitchen.bean.entity.RecipeEntity;
import cn.miyf.kitchen.bean.entity.UserEntity;
import cn.miyf.kitchen.bean.model.cooking.CookingStepTimerCalculator;
import cn.miyf.kitchen.bean.model.cooking.CookingTaskSnapshotFactory;
import cn.miyf.kitchen.bean.model.cooking.CookingTaskSorter;
import cn.miyf.kitchen.bean.model.cooking.CookingTaskStatus;
import cn.miyf.kitchen.bean.model.cooking.CookingTaskStatusMachine;
import cn.miyf.kitchen.bean.model.cooking.CookingTaskStepStatus;
import cn.miyf.kitchen.bean.model.cooking.CookingTaskStepStatusMachine;
import cn.miyf.kitchen.bean.model.template.DishTemplateStatus;
import cn.miyf.kitchen.bean.qo.CookingTaskPageQo;
import cn.miyf.kitchen.bean.vo.CookingTaskStepVo;
import cn.miyf.kitchen.bean.vo.CookingTaskVo;
import cn.miyf.kitchen.repository.CookingTaskRepository;
import cn.miyf.kitchen.repository.CookingTaskStepRepository;
import cn.miyf.kitchen.repository.DishRepository;
import cn.miyf.kitchen.repository.OperationLogRepository;
import cn.miyf.kitchen.repository.RecipeRepository;
import cn.miyf.kitchen.repository.UserRepository;
import cn.miyf.service.BaseApplicationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 组织协作做菜任务应用服务。
 * <p>
 * 任务提交时固化菜品快照；领取使用条件 UPDATE 防并发双领；
 * 状态流转统一经过 {@link CookingTaskStatusMachine}；任务池排序由
 * {@link CookingTaskSorter} 独立实现，便于后续替换。
 *
 * @author XieMingJie
 * @since 2026-10-31 15:00
 * @history 1.00 2026-10-31 15:00 XieMingJie Created.
 */
@Service
@RequiredArgsConstructor
public class CookingTaskApplicationService extends BaseApplicationService {

    private static final Set<String> SCOPES = Set.of("pool", "submitted", "assigned", "all");
    private static final Set<String> SELECTABLE_TEMPLATE_STATUSES = Set.of(
            DishTemplateStatus.PRE_CREATED, DishTemplateStatus.PUBLISHED);
    private static final int POOL_LIMIT = 500;

    private final CookingTaskRepository cookingTaskRepository;
    private final CookingTaskStepRepository cookingTaskStepRepository;
    private final DishRepository dishRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final OperationLogRepository operationLogRepository;
    private final KitchenAccessService kitchenAccessService;

    /**
     * 提交做菜任务：校验组织与模板状态，固化菜品快照并初始化步骤计时记录。
     * <p>
     * {@code submitToken} 相同且已存在任务时直接返回原任务 ID，保证重复点击幂等。
     *
     * @param dto 提交请求
     * @return 任务 ID
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    @Transactional
    public Long create(CookingTaskCreateDto dto) {
        KitchenEntity kitchen = kitchenAccessService.requireMemberKitchen();
        Long orgUnitId = kitchenAccessService.requireOrgUnitId(kitchen);
        Long currentUserId = SecurityUtils.currentUserId();
        String submitToken = trimOptional(dto.getSubmitToken(), 64, "提交令牌");
        if (submitToken != null) {
            CookingTaskEntity existing = cookingTaskRepository.selectBySubmitToken(submitToken);
            if (existing != null) {
                requireTrue(orgUnitId.equals(existing.getOrgUnitId()), ErrorCode.FORBIDDEN,
                        "无权访问该做菜任务");
                return existing.getId();
            }
        }

        DishEntity dish = requireById(dishRepository, dto.getDishId(), "菜品不存在");
        requireTrue(Boolean.TRUE.equals(dish.getIsTemplate()), ErrorCode.NOT_FOUND, "菜品模板不存在");
        requireTrue(orgUnitId.equals(dish.getOrgUnitId()), ErrorCode.FORBIDDEN, "无权使用该菜品");
        requireTrue(SELECTABLE_TEMPLATE_STATUSES.contains(dish.getTemplateStatus()),
                ErrorCode.INVALID_STATUS, "仅预创建或已发布的菜品可提交做菜任务");

        RecipeEntity recipe = recipeRepository.selectOne(
                new LambdaQueryWrapper<RecipeEntity>().eq(RecipeEntity::getDishId, dish.getId()));
        Map<String, Object> snapshot = CookingTaskSnapshotFactory.build(dish, recipe);
        int servings = resolveServings(dto.getServings(), recipe);
        int priority = dto.getPriority() == null ? 0 : dto.getPriority();
        requireTrue(priority >= 0 && priority <= 9, ErrorCode.BAD_REQUEST, "优先级仅支持 0-9");

        CookingTaskEntity task = new CookingTaskEntity()
                .setKitchenId(kitchen.getId())
                .setOrgUnitId(orgUnitId)
                .setDishId(dish.getId())
                .setDishSnapshot(snapshot)
                .setSubmitterId(currentUserId)
                .setServings(servings)
                .setNote(trimOptional(dto.getNote(), 500, "备注"))
                .setPriority(priority)
                .setClaimMode("PUBLIC")
                .setScheduledStartTime(dto.getScheduledStartTime())
                .setStatus(CookingTaskStatus.PENDING.name())
                .setCurrentStep(0)
                .setTotalPausedSeconds(0)
                .setSubmitToken(submitToken);

        try {
            insert(cookingTaskRepository, task);
        } catch (DuplicateKeyException e) {
            // 并发重复提交：唯一索引兜底；事务已被数据库标记失败，此处只返回冲突提示
            throw new BusinessException(ErrorCode.CONFLICT, "任务已提交，请勿重复操作");
        }
        saveSteps(task.getId(), snapshot);
        logAction("CREATE", task.getId(), "提交做菜任务：" + dish.getName());
        return task.getId();
    }

    /**
     * 做菜任务分页。
     * <p>
     * 任务池按 {@link CookingTaskSorter} 排序后在内存分页；其余范围按创建时间倒序。
     *
     * @param qo 查询条件
     * @return 分页结果
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    public PageResult<CookingTaskVo> page(CookingTaskPageQo qo) {
        KitchenEntity kitchen = kitchenAccessService.requireMemberKitchen();
        Long orgUnitId = kitchenAccessService.requireOrgUnitId(kitchen);
        Long currentUserId = SecurityUtils.currentUserId();
        String scope = normalizeScope(qo.getScope());
        String status = normalizeStatus(qo.getStatus());
        long page = pageOf(qo);
        long pageSize = pageSizeOf(qo);

        if ("pool".equals(scope)) {
            return poolPage(orgUnitId, currentUserId, status, qo.getKeyword(), page, pageSize);
        }

        Long submitterId = "submitted".equals(scope) ? currentUserId : null;
        Long chefId = "assigned".equals(scope) ? currentUserId : null;
        long offset = offset(page, pageSize);
        List<CookingTaskEntity> entities = cookingTaskRepository.selectTaskPage(
                orgUnitId, status, submitterId, chefId, offset, pageSize);
        long total = cookingTaskRepository.countTaskPage(orgUnitId, status, submitterId, chefId);
        if (entities.isEmpty()) {
            return emptyPage(page, pageSize);
        }
        return PageResult.of(toVoList(entities, currentUserId), total, page, pageSize);
    }

    /**
     * 任务详情：含快照与步骤计时状态。
     *
     * @param id 任务 ID
     * @return 任务 VO
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    public CookingTaskVo detail(Long id) {
        KitchenEntity kitchen = kitchenAccessService.requireMemberKitchen();
        Long orgUnitId = kitchenAccessService.requireOrgUnitId(kitchen);
        CookingTaskEntity task = requireTask(id, orgUnitId);
        List<CookingTaskStepEntity> steps = cookingTaskStepRepository.selectByTaskId(id);
        return toVo(task, loadUsers(List.of(task)), SecurityUtils.currentUserId(), steps);
    }

    /**
     * 领取任务：原子条件更新，只有一个厨师能领取成功。
     * <p>
     * 重复点击且当前用户已是领取人时按幂等返回。
     *
     * @param id 任务 ID
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    @Transactional
    public void claim(Long id) {
        KitchenEntity kitchen = kitchenAccessService.requireMemberKitchen();
        Long orgUnitId = kitchenAccessService.requireOrgUnitId(kitchen);
        Long currentUserId = SecurityUtils.currentUserId();
        CookingTaskEntity task = requireTask(id, orgUnitId);
        CookingTaskStatus status = CookingTaskStatus.from(task.getStatus());
        if (currentUserId.equals(task.getChefId()) && !status.terminal()) {
            return;
        }
        requireTrue(CookingTaskStatusMachine.canClaim(status), ErrorCode.INVALID_STATUS,
                "任务已被领取或当前状态不可领取");
        int rows = cookingTaskRepository.claim(id, currentUserId);
        requireTrue(rows > 0, ErrorCode.CONFLICT, "任务已被他人领取，请刷新");
        logAction("CLAIM", id, "领取做菜任务");
    }

    /**
     * 释放任务回任务池：仅领取厨师在开始制作前可操作。
     *
     * @param id 任务 ID
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    @Transactional
    public void release(Long id) {
        KitchenEntity kitchen = kitchenAccessService.requireMemberKitchen();
        Long orgUnitId = kitchenAccessService.requireOrgUnitId(kitchen);
        CookingTaskEntity task = requireTask(id, orgUnitId);
        Long currentUserId = SecurityUtils.currentUserId();
        if (task.getChefId() == null && CookingTaskStatus.PENDING.name().equals(task.getStatus())) {
            return;
        }
        requireTrue(Objects.equals(currentUserId, task.getChefId()), ErrorCode.FORBIDDEN, "仅领取厨师可释放任务");
        CookingTaskStatus status = CookingTaskStatus.from(task.getStatus());
        requireTrue(status == CookingTaskStatus.CLAIMED || status == CookingTaskStatus.PREPARING,
                ErrorCode.INVALID_STATUS, "已开始制作，无法释放任务");
        int rows = cookingTaskRepository.release(id, currentUserId);
        requireTrue(rows > 0, ErrorCode.CONFLICT, "任务状态已变化，请刷新");
        logAction("RELEASE", id, "释放做菜任务");
    }

    /**
     * 开始制作：CLAIMED/PREPARING → COOKING。
     *
     * @param id 任务 ID
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    @Transactional
    public void start(Long id) {
        CookingTaskEntity task = requireTaskOfCurrentUser(id);
        Long userId = SecurityUtils.currentUserId();
        requireChef(task, userId);
        CookingTaskStatus status = CookingTaskStatus.from(task.getStatus());
        if (status == CookingTaskStatus.COOKING) {
            return;
        }
        CookingTaskStatusMachine.assertTransit(status, CookingTaskStatus.COOKING);
        int rows = cookingTaskRepository.start(id, userId);
        requireTrue(rows > 0, ErrorCode.CONFLICT, "任务状态已变化，请刷新");
        startFirstStep(id, userId);
        logAction("START", id, "开始做菜");
    }

    /**
     * 暂停制作：COOKING → PAUSED，记录暂停时间。
     *
     * @param id 任务 ID
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    @Transactional
    public void pause(Long id) {
        CookingTaskEntity task = requireTaskOfCurrentUser(id);
        Long userId = SecurityUtils.currentUserId();
        requireChef(task, userId);
        CookingTaskStatus status = CookingTaskStatus.from(task.getStatus());
        if (status == CookingTaskStatus.PAUSED) {
            return;
        }
        CookingTaskStatusMachine.assertTransit(status, CookingTaskStatus.PAUSED);
        int rows = cookingTaskRepository.pause(id, userId);
        requireTrue(rows > 0, ErrorCode.CONFLICT, "任务状态已变化，请刷新");
        cookingTaskStepRepository.pauseRunningSteps(id);
        logAction("PAUSE", id, "暂停做菜");
    }

    /**
     * 恢复制作：PAUSED → COOKING，累计暂停时长。
     *
     * @param id 任务 ID
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    @Transactional
    public void resume(Long id) {
        CookingTaskEntity task = requireTaskOfCurrentUser(id);
        Long userId = SecurityUtils.currentUserId();
        requireChef(task, userId);
        CookingTaskStatus status = CookingTaskStatus.from(task.getStatus());
        if (status == CookingTaskStatus.COOKING) {
            return;
        }
        CookingTaskStatusMachine.assertTransit(status, CookingTaskStatus.COOKING);
        int rows = cookingTaskRepository.resume(id, userId);
        requireTrue(rows > 0, ErrorCode.CONFLICT, "任务状态已变化，请刷新");
        cookingTaskStepRepository.resumePausedSteps(id);
        logAction("RESUME", id, "恢复做菜");
    }

    /**
     * 完成任务：COOKING/PAUSED → COMPLETED，计算实际用时；允许提前手动完成。
     *
     * @param id 任务 ID
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    @Transactional
    public void complete(Long id) {
        CookingTaskEntity task = requireTaskOfCurrentUser(id);
        Long userId = SecurityUtils.currentUserId();
        requireChef(task, userId);
        CookingTaskStatus status = CookingTaskStatus.from(task.getStatus());
        if (status == CookingTaskStatus.COMPLETED) {
            return;
        }
        CookingTaskStatusMachine.assertTransit(status, CookingTaskStatus.COMPLETED);
        int rows = cookingTaskRepository.complete(id, userId);
        requireTrue(rows > 0, ErrorCode.CONFLICT, "任务状态已变化，请刷新");
        cookingTaskStepRepository.finishActiveSteps(id);
        cookingTaskRepository.updateCurrentStep(id, 0);
        logAction("COMPLETE", id, "完成做菜");
    }

    /**
     * 取消任务：开始制作前，提交人或领取厨师可取消。
     *
     * @param id 任务 ID
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    @Transactional
    public void cancel(Long id) {
        KitchenEntity kitchen = kitchenAccessService.requireMemberKitchen();
        Long orgUnitId = kitchenAccessService.requireOrgUnitId(kitchen);
        Long userId = SecurityUtils.currentUserId();
        CookingTaskEntity task = requireTask(id, orgUnitId);
        boolean allowed = Objects.equals(userId, task.getSubmitterId())
                || Objects.equals(userId, task.getChefId());
        requireTrue(allowed, ErrorCode.FORBIDDEN, "仅提交人或领取厨师可取消任务");
        if (CookingTaskStatus.CANCELLED.name().equals(task.getStatus())) {
            return;
        }
        CookingTaskStatus status = CookingTaskStatus.from(task.getStatus());
        requireTrue(CookingTaskStatusMachine.canCancel(status), ErrorCode.INVALID_STATUS,
                "任务已开始制作，无法取消");
        int rows = cookingTaskRepository.cancel(id, userId);
        requireTrue(rows > 0, ErrorCode.CONFLICT, "任务状态已变化，请刷新");
        logAction("CANCEL", id, "取消做菜任务");
    }

    /**
     * 开始某个步骤：PENDING → RUNNING。
     * <p>
     * 要求任务处于制作中，且同一并行分组之外的前置必需步骤均已完成；
     * 重复点击且步骤已在进行中时按幂等返回最新任务详情。
     *
     * @param id        任务 ID
     * @param stepOrder 步骤序号
     * @return 最新任务详情
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    @Transactional
    public CookingTaskVo startStep(Long id, Integer stepOrder) {
        CookingTaskEntity task = requireTaskOfCurrentUser(id);
        Long userId = SecurityUtils.currentUserId();
        requireChef(task, userId);
        requireTaskCooking(task);
        List<CookingTaskStepEntity> steps = cookingTaskStepRepository.selectByTaskId(id);
        CookingTaskStepEntity step = findStep(steps, stepOrder);
        CookingTaskStepStatus status = CookingTaskStepStatus.from(step.getStatus());
        if (status == CookingTaskStepStatus.RUNNING) {
            return detail(id);
        }
        requireTrue(CookingTaskStepStatusMachine.canStart(status), ErrorCode.INVALID_STATUS,
                status == CookingTaskStepStatus.PAUSED ? "步骤已暂停，请点击继续" : "步骤已完成，如需重做请先撤销");
        requireTrue(previousStepsDone(steps, stepOrder, stepContents(task)), ErrorCode.INVALID_STATUS,
                "请先完成前面的步骤");
        int rows = cookingTaskStepRepository.start(id, stepOrder, userId);
        requireTrue(rows > 0, ErrorCode.CONFLICT, "步骤状态已变化，请刷新");
        cookingTaskRepository.updateCurrentStep(id, stepOrder);
        logAction("STEP_START", id, "开始第 " + stepOrder + " 步");
        return detail(id);
    }

    /**
     * 暂停某个步骤：RUNNING → PAUSED，按服务端时间冻结剩余时长。
     *
     * @param id        任务 ID
     * @param stepOrder 步骤序号
     * @return 最新任务详情
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    @Transactional
    public CookingTaskVo pauseStep(Long id, Integer stepOrder) {
        CookingTaskEntity task = requireTaskOfCurrentUser(id);
        Long userId = SecurityUtils.currentUserId();
        requireChef(task, userId);
        requireTaskCooking(task);
        CookingTaskStepEntity step = findStep(cookingTaskStepRepository.selectByTaskId(id), stepOrder);
        CookingTaskStepStatus status = CookingTaskStepStatus.from(step.getStatus());
        if (status == CookingTaskStepStatus.PAUSED) {
            return detail(id);
        }
        requireTrue(CookingTaskStepStatusMachine.canPause(status), ErrorCode.INVALID_STATUS,
                "只有进行中的步骤可以暂停");
        int rows = cookingTaskStepRepository.pause(id, stepOrder, userId);
        requireTrue(rows > 0, ErrorCode.CONFLICT, "步骤状态已变化，请刷新");
        logAction("STEP_PAUSE", id, "暂停第 " + stepOrder + " 步");
        return detail(id);
    }

    /**
     * 继续某个步骤：PAUSED → RUNNING，累计暂停时长。
     *
     * @param id        任务 ID
     * @param stepOrder 步骤序号
     * @return 最新任务详情
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    @Transactional
    public CookingTaskVo resumeStep(Long id, Integer stepOrder) {
        CookingTaskEntity task = requireTaskOfCurrentUser(id);
        Long userId = SecurityUtils.currentUserId();
        requireChef(task, userId);
        requireTaskCooking(task);
        CookingTaskStepEntity step = findStep(cookingTaskStepRepository.selectByTaskId(id), stepOrder);
        CookingTaskStepStatus status = CookingTaskStepStatus.from(step.getStatus());
        if (status == CookingTaskStepStatus.RUNNING) {
            return detail(id);
        }
        requireTrue(CookingTaskStepStatusMachine.canResume(status), ErrorCode.INVALID_STATUS,
                "只有已暂停的步骤可以继续");
        int rows = cookingTaskStepRepository.resume(id, stepOrder, userId);
        requireTrue(rows > 0, ErrorCode.CONFLICT, "步骤状态已变化，请刷新");
        logAction("STEP_RESUME", id, "继续第 " + stepOrder + " 步");
        return detail(id);
    }

    /**
     * 完成某个步骤：未完成状态 → DONE。
     * <p>
     * 所有必需步骤完成后自动把任务置为已完成；否则把当前步骤指向下一个未完成步骤。
     *
     * @param id        任务 ID
     * @param stepOrder 步骤序号
     * @return 最新任务详情
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    @Transactional
    public CookingTaskVo completeStep(Long id, Integer stepOrder) {
        CookingTaskEntity task = requireTaskOfCurrentUser(id);
        Long userId = SecurityUtils.currentUserId();
        requireChef(task, userId);
        requireTaskCooking(task);
        List<CookingTaskStepEntity> steps = cookingTaskStepRepository.selectByTaskId(id);
        CookingTaskStepEntity step = findStep(steps, stepOrder);
        CookingTaskStepStatus status = CookingTaskStepStatus.from(step.getStatus());
        if (status == CookingTaskStepStatus.DONE) {
            return detail(id);
        }
        requireTrue(CookingTaskStepStatusMachine.canComplete(status), ErrorCode.INVALID_STATUS,
                "当前步骤无法完成");
        requireTrue(previousStepsDone(steps, stepOrder, stepContents(task)), ErrorCode.INVALID_STATUS,
                "请先完成前面的步骤");
        int rows = cookingTaskStepRepository.complete(id, stepOrder, userId);
        requireTrue(rows > 0, ErrorCode.CONFLICT, "步骤状态已变化，请刷新");
        logAction("STEP_COMPLETE", id, "完成第 " + stepOrder + " 步");
        finishOrAdvance(id, userId, task);
        return detail(id);
    }

    /**
     * 撤销某个步骤：DONE → PENDING，同时把任务当前步骤指回该步骤。
     * <p>
     * 仅允许撤销最后一个已完成步骤；快照中标记为不可撤销的步骤拒绝撤销。
     *
     * @param id        任务 ID
     * @param stepOrder 步骤序号
     * @return 最新任务详情
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    @Transactional
    public CookingTaskVo undoStep(Long id, Integer stepOrder) {
        CookingTaskEntity task = requireTaskOfCurrentUser(id);
        Long userId = SecurityUtils.currentUserId();
        requireChef(task, userId);
        requireTaskCooking(task);
        List<CookingTaskStepEntity> steps = cookingTaskStepRepository.selectByTaskId(id);
        CookingTaskStepEntity step = findStep(steps, stepOrder);
        CookingTaskStepStatus status = CookingTaskStepStatus.from(step.getStatus());
        if (status == CookingTaskStepStatus.PENDING) {
            return detail(id);
        }
        requireTrue(CookingTaskStepStatusMachine.canUndo(status), ErrorCode.INVALID_STATUS,
                "只有已完成的步骤可以撤销");
        Map<Integer, Map<String, Object>> contents = stepContents(task);
        requireTrue(CookingTaskSnapshotFactory.booleanOf(contents.get(stepOrder), "canUndo", true),
                ErrorCode.INVALID_STATUS, "该步骤不允许撤销");
        requireTrue(maxDoneOrder(steps) == stepOrder, ErrorCode.INVALID_STATUS, "请先撤销后面的步骤");
        requireTrue(isUndoableTip(steps, stepOrder), ErrorCode.INVALID_STATUS, "后面的步骤已开始，无法撤销");
        int rows = cookingTaskStepRepository.undo(id, stepOrder, userId);
        requireTrue(rows > 0, ErrorCode.CONFLICT, "步骤状态已变化，请刷新");
        cookingTaskRepository.updateCurrentStep(id, stepOrder);
        logAction("STEP_UNDO", id, "撤销第 " + stepOrder + " 步");
        return detail(id);
    }

    /**
     * 开始任务时自动启动第一个未完成步骤。
     *
     * @param taskId 任务 ID
     * @param userId 掌勺人 ID
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    private void startFirstStep(Long taskId, Long userId) {
        List<CookingTaskStepEntity> steps = cookingTaskStepRepository.selectByTaskId(taskId);
        CookingTaskStepEntity first = steps.stream()
                .filter(step -> CookingTaskStepStatus.PENDING.name().equals(step.getStatus()))
                .findFirst()
                .orElse(null);
        if (first == null || first.getStepOrder() == null) {
            return;
        }
        int started = cookingTaskStepRepository.start(taskId, first.getStepOrder(), userId);
        if (started > 0) {
            cookingTaskRepository.updateCurrentStep(taskId, first.getStepOrder());
        }
    }

    /**
     * 步骤完成后的收口：全部必需步骤完成则自动完成任务，否则指向下一未完成步骤。
     *
     * @param taskId 任务 ID
     * @param userId 掌勺人 ID
     * @param task   任务实体
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    private void finishOrAdvance(Long taskId, Long userId, CookingTaskEntity task) {
        List<CookingTaskStepEntity> steps = cookingTaskStepRepository.selectByTaskId(taskId);
        Map<Integer, Map<String, Object>> contents = stepContents(task);
        if (allRequiredDone(steps, contents)) {
            cookingTaskStepRepository.finishActiveSteps(taskId);
            int rows = cookingTaskRepository.complete(taskId, userId);
            if (rows > 0) {
                cookingTaskRepository.updateCurrentStep(taskId, 0);
                logAction("COMPLETE", taskId, "全部步骤完成，自动完成做菜");
            }
            return;
        }
        cookingTaskRepository.updateCurrentStep(taskId, nextStepOrder(steps));
    }

    private CookingTaskStepEntity findStep(List<CookingTaskStepEntity> steps, Integer stepOrder) {
        requireTrue(stepOrder != null && stepOrder > 0, ErrorCode.BAD_REQUEST, "步骤序号不合法");
        return steps.stream()
                .filter(step -> stepOrder.equals(step.getStepOrder()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "步骤不存在"));
    }

    private void requireTaskCooking(CookingTaskEntity task) {
        CookingTaskStatus status = CookingTaskStatus.from(task.getStatus());
        requireTrue(status == CookingTaskStatus.COOKING, ErrorCode.INVALID_STATUS,
                status == CookingTaskStatus.PAUSED ? "任务已暂停，请先继续做菜" : "请先开始做菜");
    }

    private Map<Integer, Map<String, Object>> stepContents(CookingTaskEntity task) {
        Map<Integer, Map<String, Object>> contentByOrder = new HashMap<>();
        for (Map<String, Object> content : CookingTaskSnapshotFactory.stepsOf(task.getDishSnapshot())) {
            Integer order = CookingTaskSnapshotFactory.intOf(content, "order");
            if (order != null) {
                contentByOrder.put(order, content);
            }
        }
        return contentByOrder;
    }

    /**
     * 判断前置必需步骤是否已完成；同一并行分组内的步骤互不阻塞。
     *
     * @param steps      全部步骤
     * @param targetOrder 目标步骤序号
     * @param contents   快照步骤内容
     * @return true 表示可以开始
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    private boolean previousStepsDone(List<CookingTaskStepEntity> steps,
                                      Integer targetOrder,
                                      Map<Integer, Map<String, Object>> contents) {
        Map<Integer, String> groupByOrder = new HashMap<>();
        for (CookingTaskStepEntity step : steps) {
            groupByOrder.put(step.getStepOrder(),
                    CookingTaskSnapshotFactory.textOf(contents.get(step.getStepOrder()), "parallelGroup"));
        }
        String targetGroup = groupByOrder.get(targetOrder);
        for (CookingTaskStepEntity step : steps) {
            Integer order = step.getStepOrder();
            if (order == null || order >= targetOrder) {
                continue;
            }
            String group = groupByOrder.get(order);
            if (targetGroup != null && targetGroup.equals(group)) {
                continue;
            }
            boolean required = CookingTaskSnapshotFactory.booleanOf(contents.get(order), "required", true);
            if (required && !CookingTaskStepStatus.DONE.name().equals(step.getStatus())) {
                return false;
            }
        }
        return true;
    }

    private int maxDoneOrder(List<CookingTaskStepEntity> steps) {
        int max = 0;
        for (CookingTaskStepEntity step : steps) {
            if (CookingTaskStepStatus.DONE.name().equals(step.getStatus())
                    && step.getStepOrder() != null && step.getStepOrder() > max) {
                max = step.getStepOrder();
            }
        }
        return max;
    }

    /**
     * 是否可以从该步骤撤销：后续步骤必须都未开始。
     *
     * @param steps     全部步骤
     * @param stepOrder 目标步骤序号
     * @return true 表示可以撤销
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    private boolean isUndoableTip(List<CookingTaskStepEntity> steps, Integer stepOrder) {
        for (CookingTaskStepEntity step : steps) {
            if (step.getStepOrder() == null || step.getStepOrder() <= stepOrder) {
                continue;
            }
            if (!CookingTaskStepStatus.PENDING.name().equals(step.getStatus())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 是否所有必需步骤均已完成；若没有必需步骤，则以全部步骤完成作为判断。
     *
     * @param steps    全部步骤
     * @param contents 快照步骤内容
     * @return true 表示全部必需步骤完成
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    private boolean allRequiredDone(List<CookingTaskStepEntity> steps,
                                    Map<Integer, Map<String, Object>> contents) {
        boolean hasRequired = false;
        for (CookingTaskStepEntity step : steps) {
            boolean required = CookingTaskSnapshotFactory.booleanOf(
                    contents.get(step.getStepOrder()), "required", true);
            if (!required) {
                continue;
            }
            hasRequired = true;
            if (!CookingTaskStepStatus.DONE.name().equals(step.getStatus())) {
                return false;
            }
        }
        if (hasRequired) {
            return true;
        }
        return !steps.isEmpty()
                && steps.stream().allMatch(step -> CookingTaskStepStatus.DONE.name().equals(step.getStatus()));
    }

    private int nextStepOrder(List<CookingTaskStepEntity> steps) {
        for (CookingTaskStepEntity step : steps) {
            if (!CookingTaskStepStatus.DONE.name().equals(step.getStatus())
                    && step.getStepOrder() != null) {
                return step.getStepOrder();
            }
        }
        return 0;
    }

    private PageResult<CookingTaskVo> poolPage(Long orgUnitId,
                                               Long currentUserId,
                                               String status,
                                               String keyword,
                                               long page,
                                               long pageSize) {
        List<CookingTaskEntity> pool = cookingTaskRepository.selectPool(orgUnitId, currentUserId, POOL_LIMIT);
        String keywordLower = keyword == null ? null : keyword.trim().toLowerCase(Locale.ROOT);
        List<CookingTaskEntity> filtered = pool.stream()
                .filter(task -> status == null || status.equals(task.getStatus()))
                .filter(task -> matchesKeyword(task, keywordLower))
                .toList();
        List<CookingTaskEntity> sorted = CookingTaskSorter.sort(filtered);
        int from = (int) Math.min(offset(page, pageSize), sorted.size());
        int to = (int) Math.min(from + pageSize, sorted.size());
        List<CookingTaskEntity> records = sorted.subList(from, to);
        return PageResult.of(toVoList(records, currentUserId), sorted.size(), page, pageSize);
    }

    private boolean matchesKeyword(CookingTaskEntity task, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String name = CookingTaskSnapshotFactory.textOf(task.getDishSnapshot(), "name");
        if (name != null && name.toLowerCase(Locale.ROOT).contains(keyword)) {
            return true;
        }
        return task.getNote() != null && task.getNote().toLowerCase(Locale.ROOT).contains(keyword);
    }

    private int resolveServings(Integer requested, RecipeEntity recipe) {
        int servings = requested != null
                ? requested
                : recipe != null && recipe.getServings() != null ? recipe.getServings() : 1;
        requireTrue(servings >= 1 && servings <= 99, ErrorCode.BAD_REQUEST, "份数仅支持 1-99");
        return servings;
    }

    /**
     * 初始化任务步骤计时记录；步骤内容来自快照，不复制到步骤表。
     *
     * @param taskId   任务 ID
     * @param snapshot 菜品快照
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    private void saveSteps(Long taskId, Map<String, Object> snapshot) {
        List<Map<String, Object>> steps = CookingTaskSnapshotFactory.stepsOf(snapshot);
        List<CookingTaskStepEntity> entities = new ArrayList<>(steps.size());
        int order = 1;
        for (Map<String, Object> step : steps) {
            String timerType = CookingTaskSnapshotFactory.textOf(step, "timerType");
            if (timerType == null) {
                timerType = "none";
            }
            Integer duration = CookingTaskSnapshotFactory.intOf(step, "durationSeconds");
            Integer remaining = "countdown".equals(timerType) && duration != null ? duration : null;
            entities.add(new CookingTaskStepEntity()
                    .setTaskId(taskId)
                    .setStepOrder(order)
                    .setStatus("PENDING")
                    .setTimerType(timerType)
                    .setDurationSeconds(duration)
                    .setTotalPausedSeconds(0)
                    .setRemainingSeconds(remaining));
            order++;
        }
        insertAll(cookingTaskStepRepository, entities);
    }

    private List<CookingTaskVo> toVoList(List<CookingTaskEntity> entities, Long currentUserId) {
        Map<Long, UserEntity> users = loadUsers(entities);
        List<CookingTaskVo> records = new ArrayList<>(entities.size());
        for (CookingTaskEntity entity : entities) {
            records.add(toVo(entity, users, currentUserId, null));
        }
        return records;
    }

    private Map<Long, UserEntity> loadUsers(List<CookingTaskEntity> entities) {
        List<Long> ids = entities.stream()
                .flatMap(task -> Stream.of(task.getSubmitterId(), task.getChefId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity(), (a, b) -> a));
    }

    private CookingTaskVo toVo(CookingTaskEntity task,
                               Map<Long, UserEntity> users,
                               Long currentUserId,
                               List<CookingTaskStepEntity> stepEntities) {
        CookingTaskStatus status = CookingTaskStatus.from(task.getStatus());
        boolean chefMe = Objects.equals(currentUserId, task.getChefId());
        boolean submitterMe = Objects.equals(currentUserId, task.getSubmitterId());
        UserEntity submitter = task.getSubmitterId() == null ? null : users.get(task.getSubmitterId());
        UserEntity chef = task.getChefId() == null ? null : users.get(task.getChefId());
        CookingTaskVo vo = new CookingTaskVo()
                .setId(task.getId())
                .setKitchenId(task.getKitchenId())
                .setOrgUnitId(task.getOrgUnitId())
                .setDishId(task.getDishId())
                .setDishSnapshot(task.getDishSnapshot())
                .setSubmitterId(task.getSubmitterId())
                .setSubmitterName(submitter == null ? null : submitter.getNickname())
                .setChefId(task.getChefId())
                .setChefName(chef == null ? null : chef.getNickname())
                .setServings(task.getServings())
                .setNote(task.getNote())
                .setPriority(task.getPriority())
                .setClaimMode(task.getClaimMode())
                .setScheduledStartTime(task.getScheduledStartTime())
                .setStatus(task.getStatus())
                .setCurrentStep(task.getCurrentStep())
                .setStartedTime(task.getStartedTime())
                .setPausedTime(task.getPausedTime())
                .setCompletedTime(task.getCompletedTime())
                .setTotalPausedSeconds(task.getTotalPausedSeconds())
                .setActualDurationSeconds(task.getActualDurationSeconds())
                .setCreateTime(task.getCreateTime())
                .setLastModifyTime(task.getLastModifyTime())
                .setCanClaim(status == CookingTaskStatus.PENDING && task.getChefId() == null)
                .setCanStart(chefMe
                        && (status == CookingTaskStatus.CLAIMED || status == CookingTaskStatus.PREPARING))
                .setCanPause(chefMe && status == CookingTaskStatus.COOKING)
                .setCanResume(chefMe && status == CookingTaskStatus.PAUSED)
                .setCanComplete(chefMe
                        && (status == CookingTaskStatus.COOKING || status == CookingTaskStatus.PAUSED))
                .setCanRelease(chefMe
                        && (status == CookingTaskStatus.CLAIMED || status == CookingTaskStatus.PREPARING))
                .setCanCancel(CookingTaskStatusMachine.canCancel(status) && (submitterMe || chefMe))
                .setClaimedByMe(chefMe)
                .setCanOpenFlow(chefMe && status != CookingTaskStatus.PENDING
                        && status != CookingTaskStatus.CANCELLED)
                .setServerTime(Instant.now());
        if (stepEntities != null) {
            vo.setSteps(toStepVos(task, stepEntities, currentUserId));
        }
        return vo;
    }

    private List<CookingTaskStepVo> toStepVos(CookingTaskEntity task,
                                              List<CookingTaskStepEntity> entities,
                                              Long currentUserId) {
        Map<Integer, Map<String, Object>> contentByOrder = stepContents(task);
        int currentStep = task.getCurrentStep() == null ? 0 : task.getCurrentStep();
        boolean taskCooking = CookingTaskStatus.COOKING.name().equals(task.getStatus());
        boolean chefMe = Objects.equals(currentUserId, task.getChefId());
        int lastDoneOrder = maxDoneOrder(entities);
        Instant now = Instant.now();
        List<CookingTaskStepVo> steps = new ArrayList<>(entities.size());
        for (CookingTaskStepEntity entity : entities) {
            Map<String, Object> content = contentByOrder.getOrDefault(entity.getStepOrder(), Map.of());
            CookingTaskStepStatus stepStatus = CookingTaskStepStatus.from(entity.getStatus());
            boolean canStart = taskCooking && chefMe
                    && CookingTaskStepStatusMachine.canStart(stepStatus)
                    && previousStepsDone(entities, entity.getStepOrder(), contentByOrder);
            boolean canComplete = taskCooking && chefMe
                    && CookingTaskStepStatusMachine.canComplete(stepStatus)
                    && (stepStatus != CookingTaskStepStatus.PENDING
                        || previousStepsDone(entities, entity.getStepOrder(), contentByOrder));
            boolean canUndoNow = taskCooking && chefMe
                    && CookingTaskStepStatusMachine.canUndo(stepStatus)
                    && entity.getStepOrder() != null
                    && entity.getStepOrder() == lastDoneOrder
                    && isUndoableTip(entities, entity.getStepOrder())
                    && CookingTaskSnapshotFactory.booleanOf(content, "canUndo", true);
            steps.add(new CookingTaskStepVo()
                    .setId(entity.getId())
                    .setStepOrder(entity.getStepOrder())
                    .setStatus(entity.getStatus())
                    .setTimerType(entity.getTimerType())
                    .setDurationSeconds(entity.getDurationSeconds())
                    .setRemainingSeconds(CookingStepTimerCalculator.remainingSeconds(entity, now))
                    .setElapsedSeconds(CookingStepTimerCalculator.elapsedSeconds(entity, now))
                    .setStartedTime(entity.getStartedTime())
                    .setPausedTime(entity.getPausedTime())
                    .setTotalPausedSeconds(entity.getTotalPausedSeconds())
                    .setCompletedTime(entity.getCompletedTime())
                    .setTitle(CookingTaskSnapshotFactory.textOf(content, "title"))
                    .setDescription(CookingTaskSnapshotFactory.textOf(content, "description"))
                    .setImage(CookingTaskSnapshotFactory.textOf(content, "image"))
                    .setVideo(CookingTaskSnapshotFactory.textOf(content, "video"))
                    .setParallelGroup(CookingTaskSnapshotFactory.textOf(content, "parallelGroup"))
                    .setCompletionHint(CookingTaskSnapshotFactory.textOf(content, "completionHint"))
                    .setWarning(CookingTaskSnapshotFactory.textOf(content, "warning"))
                    .setRequired(CookingTaskSnapshotFactory.booleanOf(content, "required", true))
                    .setCanUndo(CookingTaskSnapshotFactory.booleanOf(content, "canUndo", true))
                    .setCurrent(entity.getStepOrder() != null && entity.getStepOrder() == currentStep)
                    .setCanStart(canStart)
                    .setCanPause(taskCooking && chefMe
                            && CookingTaskStepStatusMachine.canPause(stepStatus))
                    .setCanResume(taskCooking && chefMe
                            && CookingTaskStepStatusMachine.canResume(stepStatus))
                    .setCanComplete(canComplete)
                    .setCanUndoNow(canUndoNow));
        }
        return steps;
    }

    private CookingTaskEntity requireTaskOfCurrentUser(Long id) {
        KitchenEntity kitchen = kitchenAccessService.requireMemberKitchen();
        Long orgUnitId = kitchenAccessService.requireOrgUnitId(kitchen);
        return requireTask(id, orgUnitId);
    }

    private CookingTaskEntity requireTask(Long id, Long orgUnitId) {
        CookingTaskEntity task = requireById(cookingTaskRepository, id, "做菜任务不存在");
        requireTrue(orgUnitId.equals(task.getOrgUnitId()), ErrorCode.FORBIDDEN, "无权访问该做菜任务");
        return task;
    }

    private void requireChef(CookingTaskEntity task, Long currentUserId) {
        requireTrue(Objects.equals(currentUserId, task.getChefId()), ErrorCode.FORBIDDEN,
                "仅领取厨师可操作该任务");
    }

    private String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return "pool";
        }
        String normalized = scope.trim().toLowerCase(Locale.ROOT);
        requireTrue(SCOPES.contains(normalized), ErrorCode.BAD_REQUEST,
                "范围仅支持 pool/submitted/assigned/all");
        return normalized;
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return CookingTaskStatus.from(status).name();
    }

    private String trimOptional(String value, int max, String label) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        requireTrue(trimmed.length() <= max, ErrorCode.BAD_REQUEST,
                label + "不能超过 " + max + " 个字符");
        return trimmed;
    }

    private void logAction(String action, Long taskId, String detail) {
        AuthPrincipal principal = SecurityUtils.currentPrincipal().orElse(null);
        operationLogRepository.insert(new OperationLogEntity()
                .setOperatorId(principal == null ? null : principal.getId())
                .setOperatorName(principal == null ? null : principal.getUsername())
                .setOperationType("COOKING_TASK_" + action)
                .setTargetType("COOKING_TASK")
                .setTargetId(String.valueOf(taskId))
                .setOperationDetail(detail));
    }
}
