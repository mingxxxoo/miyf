package cn.miyf.kitchen.controller.personal;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.dto.CookingTaskCreateDto;
import cn.miyf.kitchen.bean.qo.CookingTaskPageQo;
import cn.miyf.kitchen.bean.vo.CookingTaskVo;
import cn.miyf.kitchen.security.KitchenPersonalPopedom;
import cn.miyf.kitchen.service.CookingTaskApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 组织协作做菜任务成员端接口。
 * <p>
 * 所有接口按组织单元隔离；领取只允许一个厨师成功，重复点击由服务端幂等处理。
 *
 * @author XieMingJie
 * @since 2026-10-31 15:00
 * @history 1.00 2026-10-31 15:00 XieMingJie Created.
 */
@Tag(name = "做菜任务")
@KitchenPersonalPopedom
@RestController
@RequestMapping("/cooking-tasks")
@RequiredArgsConstructor
public class CookingTaskController {

    private final CookingTaskApplicationService cookingTaskApplicationService;

    /**
     * 提交做菜任务。
     *
     * @param dto 提交请求
     * @return 任务 ID
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    @Operation(summary = "提交做菜任务")
    @MiyfPermission(code = "kitchen:user:cooking-task:create")
    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody CookingTaskCreateDto dto) {
        return ApiResult.ok(cookingTaskApplicationService.create(dto));
    }

    /**
     * 做菜任务分页：任务池 / 我提交的 / 我领取的 / 组织全部。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    @Operation(summary = "做菜任务分页")
    @MiyfPermission(code = "kitchen:user:cooking-task:list")
    @GetMapping
    public ApiResult<PageResult<CookingTaskVo>> page(CookingTaskPageQo qo) {
        return ApiResult.ok(cookingTaskApplicationService.page(qo));
    }

    /**
     * 做菜任务详情（含快照与步骤计时状态）。
     *
     * @param id 任务 ID
     * @return 任务
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    @Operation(summary = "做菜任务详情")
    @MiyfPermission(code = "kitchen:user:cooking-task:detail")
    @GetMapping("/{id}")
    public ApiResult<CookingTaskVo> detail(@PathVariable Long id) {
        return ApiResult.ok(cookingTaskApplicationService.detail(id));
    }

    /**
     * 领取任务（原子操作，防并发双领）。
     *
     * @param id 任务 ID
     * @return 空结果
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    @Operation(summary = "领取做菜任务")
    @MiyfPermission(code = "kitchen:user:cooking-task:claim")
    @PostMapping("/{id}/claim")
    public ApiResult<Void> claim(@PathVariable Long id) {
        cookingTaskApplicationService.claim(id);
        return ApiResult.ok();
    }

    /**
     * 释放任务回任务池（开始制作前）。
     *
     * @param id 任务 ID
     * @return 空结果
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    @Operation(summary = "释放做菜任务")
    @MiyfPermission(code = "kitchen:user:cooking-task:operate")
    @PostMapping("/{id}/release")
    public ApiResult<Void> release(@PathVariable Long id) {
        cookingTaskApplicationService.release(id);
        return ApiResult.ok();
    }

    /**
     * 开始制作。
     *
     * @param id 任务 ID
     * @return 空结果
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    @Operation(summary = "开始做菜")
    @MiyfPermission(code = "kitchen:user:cooking-task:operate")
    @PostMapping("/{id}/start")
    public ApiResult<Void> start(@PathVariable Long id) {
        cookingTaskApplicationService.start(id);
        return ApiResult.ok();
    }

    /**
     * 暂停制作。
     *
     * @param id 任务 ID
     * @return 空结果
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    @Operation(summary = "暂停做菜")
    @MiyfPermission(code = "kitchen:user:cooking-task:operate")
    @PostMapping("/{id}/pause")
    public ApiResult<Void> pause(@PathVariable Long id) {
        cookingTaskApplicationService.pause(id);
        return ApiResult.ok();
    }

    /**
     * 恢复制作。
     *
     * @param id 任务 ID
     * @return 空结果
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    @Operation(summary = "恢复做菜")
    @MiyfPermission(code = "kitchen:user:cooking-task:operate")
    @PostMapping("/{id}/resume")
    public ApiResult<Void> resume(@PathVariable Long id) {
        cookingTaskApplicationService.resume(id);
        return ApiResult.ok();
    }

    /**
     * 完成任务（允许提前手动完成）。
     *
     * @param id 任务 ID
     * @return 空结果
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    @Operation(summary = "完成做菜")
    @MiyfPermission(code = "kitchen:user:cooking-task:operate")
    @PostMapping("/{id}/complete")
    public ApiResult<Void> complete(@PathVariable Long id) {
        cookingTaskApplicationService.complete(id);
        return ApiResult.ok();
    }

    /**
     * 开始某个步骤：PENDING → RUNNING。
     *
     * @param id        任务 ID
     * @param stepOrder 步骤序号
     * @return 最新任务详情
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    @Operation(summary = "开始步骤")
    @MiyfPermission(code = "kitchen:user:cooking-task:operate")
    @PostMapping("/{id}/steps/{stepOrder}/start")
    public ApiResult<CookingTaskVo> startStep(@PathVariable Long id, @PathVariable Integer stepOrder) {
        return ApiResult.ok(cookingTaskApplicationService.startStep(id, stepOrder));
    }

    /**
     * 暂停某个步骤：RUNNING → PAUSED。
     *
     * @param id        任务 ID
     * @param stepOrder 步骤序号
     * @return 最新任务详情
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    @Operation(summary = "暂停步骤")
    @MiyfPermission(code = "kitchen:user:cooking-task:operate")
    @PostMapping("/{id}/steps/{stepOrder}/pause")
    public ApiResult<CookingTaskVo> pauseStep(@PathVariable Long id, @PathVariable Integer stepOrder) {
        return ApiResult.ok(cookingTaskApplicationService.pauseStep(id, stepOrder));
    }

    /**
     * 继续某个步骤：PAUSED → RUNNING。
     *
     * @param id        任务 ID
     * @param stepOrder 步骤序号
     * @return 最新任务详情
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    @Operation(summary = "继续步骤")
    @MiyfPermission(code = "kitchen:user:cooking-task:operate")
    @PostMapping("/{id}/steps/{stepOrder}/resume")
    public ApiResult<CookingTaskVo> resumeStep(@PathVariable Long id, @PathVariable Integer stepOrder) {
        return ApiResult.ok(cookingTaskApplicationService.resumeStep(id, stepOrder));
    }

    /**
     * 完成某个步骤：未完成状态 → DONE，全部必需步骤完成后自动完成任务。
     *
     * @param id        任务 ID
     * @param stepOrder 步骤序号
     * @return 最新任务详情
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    @Operation(summary = "完成步骤")
    @MiyfPermission(code = "kitchen:user:cooking-task:operate")
    @PostMapping("/{id}/steps/{stepOrder}/complete")
    public ApiResult<CookingTaskVo> completeStep(@PathVariable Long id, @PathVariable Integer stepOrder) {
        return ApiResult.ok(cookingTaskApplicationService.completeStep(id, stepOrder));
    }

    /**
     * 撤销某个步骤：DONE → PENDING。
     *
     * @param id        任务 ID
     * @param stepOrder 步骤序号
     * @return 最新任务详情
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    @Operation(summary = "撤销步骤")
    @MiyfPermission(code = "kitchen:user:cooking-task:operate")
    @PostMapping("/{id}/steps/{stepOrder}/undo")
    public ApiResult<CookingTaskVo> undoStep(@PathVariable Long id, @PathVariable Integer stepOrder) {
        return ApiResult.ok(cookingTaskApplicationService.undoStep(id, stepOrder));
    }

    /**
     * 取消任务（开始制作前）。
     *
     * @param id 任务 ID
     * @return 空结果
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    @Operation(summary = "取消做菜任务")
    @MiyfPermission(code = "kitchen:user:cooking-task:cancel")
    @PostMapping("/{id}/cancel")
    public ApiResult<Void> cancel(@PathVariable Long id) {
        cookingTaskApplicationService.cancel(id);
        return ApiResult.ok();
    }
}
