package cn.miyf.kitchen.repository;

import cn.miyf.kitchen.bean.entity.CookingTaskStepEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 做菜任务步骤数据访问接口。
 * <p>
 * 步骤开始/暂停/继续/完成/撤销全部使用带状态条件的原子 UPDATE，
 * 并把任务状态与掌勺人作为附加条件，防止任务已完成或换人后仍能修改步骤。
 *
 * @author XieMingJie
 * @since 2026-10-31 15:00
 * @history 1.00 2026-10-31 15:00 XieMingJie Created.
 */
@Mapper
public interface CookingTaskStepRepository extends BaseMapper<CookingTaskStepEntity> {

    /**
     * 按任务查询步骤，按序号升序。
     *
     * @param taskId 任务 ID
     * @return 步骤列表
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    List<CookingTaskStepEntity> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 开始步骤：PENDING → RUNNING，重置计时起点。
     *
     * @param taskId    任务 ID
     * @param stepOrder 步骤序号
     * @param chefId    掌勺人 ID
     * @return 影响行数
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    int start(@Param("taskId") Long taskId,
              @Param("stepOrder") Integer stepOrder,
              @Param("chefId") Long chefId);

    /**
     * 暂停步骤：RUNNING → PAUSED，冻结剩余时间。
     *
     * @param taskId    任务 ID
     * @param stepOrder 步骤序号
     * @param chefId    掌勺人 ID
     * @return 影响行数
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    int pause(@Param("taskId") Long taskId,
              @Param("stepOrder") Integer stepOrder,
              @Param("chefId") Long chefId);

    /**
     * 继续步骤：PAUSED → RUNNING，累计暂停时长。
     *
     * @param taskId    任务 ID
     * @param stepOrder 步骤序号
     * @param chefId    掌勺人 ID
     * @return 影响行数
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    int resume(@Param("taskId") Long taskId,
               @Param("stepOrder") Integer stepOrder,
               @Param("chefId") Long chefId);

    /**
     * 完成步骤：PENDING/RUNNING/PAUSED → DONE。
     *
     * @param taskId    任务 ID
     * @param stepOrder 步骤序号
     * @param chefId    掌勺人 ID
     * @return 影响行数
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    int complete(@Param("taskId") Long taskId,
                 @Param("stepOrder") Integer stepOrder,
                 @Param("chefId") Long chefId);

    /**
     * 撤销步骤：DONE → PENDING，清空计时。
     *
     * @param taskId    任务 ID
     * @param stepOrder 步骤序号
     * @param chefId    掌勺人 ID
     * @return 影响行数
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    int undo(@Param("taskId") Long taskId,
             @Param("stepOrder") Integer stepOrder,
             @Param("chefId") Long chefId);

    /**
     * 暂停任务下所有进行中的步骤。
     *
     * @param taskId 任务 ID
     * @return 影响行数
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    int pauseRunningSteps(@Param("taskId") Long taskId);

    /**
     * 继续任务下所有已暂停的步骤。
     *
     * @param taskId 任务 ID
     * @return 影响行数
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    int resumePausedSteps(@Param("taskId") Long taskId);

    /**
     * 结束任务时把进行中/已暂停的步骤收口为已完成。
     *
     * @param taskId 任务 ID
     * @return 影响行数
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    int finishActiveSteps(@Param("taskId") Long taskId);
}
