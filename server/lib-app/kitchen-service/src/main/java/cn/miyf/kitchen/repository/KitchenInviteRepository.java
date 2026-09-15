package cn.miyf.kitchen.repository;

import cn.miyf.kitchen.bean.entity.KitchenInviteEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 邀请码数据访问。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
@Mapper
public interface KitchenInviteRepository extends BaseMapper<KitchenInviteEntity> {

    /**
     * 厨房当前 ACTIVE 邀请。
     *
     * @param kitchenId 厨房 ID
     * @return 邀请，无则 null
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    KitchenInviteEntity selectActiveByKitchenId(@Param("kitchenId") Long kitchenId);

    /**
     * 按短码查询。
     *
     * @param code 邀请短码
     * @return 邀请，无则 null
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    KitchenInviteEntity selectByCode(@Param("code") String code);

    /**
     * 按 token 查询。
     *
     * @param token 邀请 token
     * @return 邀请，无则 null
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    KitchenInviteEntity selectByToken(@Param("token") String token);

    /**
     * 使用次数 +1。
     *
     * @param id 邀请 ID
     * @return 影响行数
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    int incrementUsedCount(@Param("id") Long id);
}
