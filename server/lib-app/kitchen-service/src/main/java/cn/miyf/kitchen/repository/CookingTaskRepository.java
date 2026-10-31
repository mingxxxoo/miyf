package cn.miyf.kitchen.repository;

import cn.miyf.kitchen.bean.entity.CookingTaskEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 做菜任务数据访问接口。
 * <p>
 * 领取与状态流转使用条件 UPDATE 保证并发原子性；任务池与分页查询见
 * {@code CookingTaskRepository.xml}。
 *
 * @author XieMingJie
 * @since 2026-10-31 15:00
 * @history 1.00 2026-10-31 15:00 XieMingJie Created.
 */
@Mapper
public interface CookingTaskRepository extends BaseMapper<CookingTaskEntity> {

    /**
     * 原子领取：仅当任务仍为 PENDING 且未被领取时生效。
     *
     * @param id     任务 ID
     * @param chefId 领取厨师 ID
     * @return 影响行数，0 表示已被他人领取或状态已变化
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    int claim(@Param("id") Long id, @Param("chefId") Long chefId);

    /**
     * 释放任务回任务池：仅领取人可释放，且尚未开始制作。
     *
     * @param id     任务 ID
     * @param chefId 领取厨师 ID
     * @return 影响行数
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    int release(@Param("id") Long id, @Param("chefId") Long chefId);

    /**
     * 开始制作：CLAIMED/PREPARING → COOKING，并写入首次开始时间。
     *
     * @param id     任务 ID
     * @param chefId 领取厨师 ID
     * @return 影响行数
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    int start(@Param("id") Long id, @Param("chefId") Long chefId);

    /**
     * 暂停制作：COOKING → PAUSED，记录暂停开始时间。
     *
     * @param id     任务 ID
     * @param chefId 领取厨师 ID
     * @return 影响行数
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    int pause(@Param("id") Long id, @Param("chefId") Long chefId);

    /**
     * 恢复制作：PAUSED → COOKING，累计暂停时长。
     *
     * @param id     任务 ID
     * @param chefId 领取厨师 ID
     * @return 影响行数
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    int resume(@Param("id") Long id, @Param("chefId") Long chefId);

    /**
     * 完成任务：COOKING/PAUSED → COMPLETED，计算实际用时。
     *
     * @param id     任务 ID
     * @param chefId 领取厨师 ID
     * @return 影响行数
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    int complete(@Param("id") Long id, @Param("chefId") Long chefId);

    /**
     * 取消任务：开始制作前，提交人或领取厨师可取消。
     *
     * @param id     任务 ID
     * @param userId 当前用户 ID
     * @return 影响行数
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    int cancel(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 更新当前步骤序号：0 表示无进行中步骤。
     *
     * @param id          任务 ID
     * @param currentStep 当前步骤序号
     * @return 影响行数
     * @history 1.00 2026-10-31 17:00 XieMingJie Created.
     */
    int updateCurrentStep(@Param("id") Long id, @Param("currentStep") Integer currentStep);

    /**
     * 按提交令牌查询，用于创建任务幂等。
     *
     * @param submitToken 提交令牌
     * @return 任务实体，不存在返回 null
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    CookingTaskEntity selectBySubmitToken(@Param("submitToken") String submitToken);

    /**
     * 任务池：组织内未完结的任务，包含未被领取的任务、本人领取/提交的任务。
     *
     * @param orgUnitId 组织单元 ID
     * @param userId    当前用户 ID
     * @param limit     最大条数
     * @return 任务列表
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    List<CookingTaskEntity> selectPool(@Param("orgUnitId") Long orgUnitId,
                                       @Param("userId") Long userId,
                                       @Param("limit") int limit);

    /**
     * 任务分页：按提交人、领取人、状态筛选，创建时间倒序。
     *
     * @param orgUnitId   组织单元 ID
     * @param status      状态，可空
     * @param submitterId 提交人，可空
     * @param chefId      领取人，可空
     * @param offset      偏移
     * @param limit       条数
     * @return 任务列表
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    List<CookingTaskEntity> selectTaskPage(@Param("orgUnitId") Long orgUnitId,
                                           @Param("status") String status,
                                           @Param("submitterId") Long submitterId,
                                           @Param("chefId") Long chefId,
                                           @Param("offset") long offset,
                                           @Param("limit") long limit);

    /**
     * 任务分页总数。
     *
     * @param orgUnitId   组织单元 ID
     * @param status      状态，可空
     * @param submitterId 提交人，可空
     * @param chefId      领取人，可空
     * @return 总数
     * @history 1.00 2026-10-31 15:00 XieMingJie Created.
     */
    long countTaskPage(@Param("orgUnitId") Long orgUnitId,
                       @Param("status") String status,
                       @Param("submitterId") Long submitterId,
                       @Param("chefId") Long chefId);
}
