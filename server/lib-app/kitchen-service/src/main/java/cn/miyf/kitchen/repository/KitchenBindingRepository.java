package cn.miyf.kitchen.repository;

import cn.miyf.kitchen.bean.entity.KitchenBindingEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 绑定数据访问。
 * 一客一厨：BOUND/PENDING 查询支撑门禁与申请幂等。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
@Mapper
public interface KitchenBindingRepository extends BaseMapper<KitchenBindingEntity> {

    /**
     * 食客当前已生效绑定（BOUND）。
     *
     * @param dinerUserId 食客用户 ID
     * @return 绑定，无则 null
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    KitchenBindingEntity selectBoundByDiner(@Param("dinerUserId") Long dinerUserId);

    /**
     * 当前关系：优先 BOUND，其次 PENDING，再次最新 REJECTED（便于展示驳回原因）。
     *
     * @param dinerUserId 食客用户 ID
     * @return 绑定，无则 null
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    KitchenBindingEntity selectActiveByDiner(@Param("dinerUserId") Long dinerUserId);

    /**
     * 指定厨房与食客的 PENDING。
     *
     * @param kitchenId   厨房 ID
     * @param dinerUserId 食客用户 ID
     * @return PENDING 绑定，无则 null
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    KitchenBindingEntity selectPending(@Param("kitchenId") Long kitchenId,
                                       @Param("dinerUserId") Long dinerUserId);

    /**
     * 食客任意厨房的 PENDING（一客同时仅一条待确认）。
     *
     * @param dinerUserId 食客用户 ID
     * @return PENDING 绑定，无则 null
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    KitchenBindingEntity selectPendingByDiner(@Param("dinerUserId") Long dinerUserId);

    /**
     * 厨房与食客最新一条绑定记录。
     *
     * @param kitchenId   厨房 ID
     * @param dinerUserId 食客用户 ID
     * @return 最新绑定，无则 null
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    KitchenBindingEntity selectLatest(@Param("kitchenId") Long kitchenId,
                                      @Param("dinerUserId") Long dinerUserId);

    /**
     * 厨师端本厨房绑定分页。
     *
     * @param kitchenId 厨房 ID
     * @param status    状态过滤，可空
     * @param offset    偏移
     * @param limit     条数
     * @return 记录列表
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    List<KitchenBindingEntity> selectChefPage(@Param("kitchenId") Long kitchenId,
                                              @Param("status") String status,
                                              @Param("offset") long offset,
                                              @Param("limit") long limit);

    /**
     * 厨师端本厨房绑定总数。
     *
     * @param kitchenId 厨房 ID
     * @param status    状态过滤，可空
     * @return 总数
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    long countChefPage(@Param("kitchenId") Long kitchenId, @Param("status") String status);

    /**
     * 管理端绑定分页。
     *
     * @param kitchenId 厨房 ID，可空
     * @param status    状态过滤，可空
     * @param offset    偏移
     * @param limit     条数
     * @return 记录列表
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    List<KitchenBindingEntity> selectAdminPage(@Param("kitchenId") Long kitchenId,
                                               @Param("status") String status,
                                               @Param("offset") long offset,
                                               @Param("limit") long limit);

    /**
     * 管理端绑定总数。
     *
     * @param kitchenId 厨房 ID，可空
     * @param status    状态过滤，可空
     * @return 总数
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    long countAdminPage(@Param("kitchenId") Long kitchenId, @Param("status") String status);
}
