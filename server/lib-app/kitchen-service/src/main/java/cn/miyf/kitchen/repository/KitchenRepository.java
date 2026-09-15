package cn.miyf.kitchen.repository;

import cn.miyf.kitchen.bean.entity.KitchenEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 厨房数据访问。
 * 一用户一厨：按 ownerUserId 唯一查询。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
@Mapper
public interface KitchenRepository extends BaseMapper<KitchenEntity> {

    /**
     * 按厨师用户 ID 查厨房。
     *
     * @param ownerUserId 厨师用户 ID
     * @return 厨房，无则 null
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    KitchenEntity selectByOwnerUserId(@Param("ownerUserId") Long ownerUserId);

    /**
     * 管理端厨房分页。
     *
     * @param keyword 名称关键字，可空
     * @param status  状态过滤，可空
     * @param offset  偏移
     * @param limit   条数
     * @return 记录列表
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    List<KitchenEntity> selectAdminPage(@Param("keyword") String keyword,
                                        @Param("status") String status,
                                        @Param("offset") long offset,
                                        @Param("limit") long limit);

    /**
     * 管理端厨房总数。
     *
     * @param keyword 名称关键字，可空
     * @param status  状态过滤，可空
     * @return 总数
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    long countAdminPage(@Param("keyword") String keyword, @Param("status") String status);
}
