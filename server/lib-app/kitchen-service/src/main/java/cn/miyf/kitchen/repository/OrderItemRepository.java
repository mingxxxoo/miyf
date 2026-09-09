package cn.miyf.kitchen.repository;

import cn.miyf.kitchen.bean.entity.OrderItemEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 预约明细数据访问接口（MyBatis Mapper）。
 * 明细随预约头一起由 Service 写入；查询详情时按 orderId 装载。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Mapper
public interface OrderItemRepository extends BaseMapper<OrderItemEntity> {

    /**
     * 按预约单查询明细列表。
     *
     * @param orderId 预约 ID
     * @return 明细列表
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    List<OrderItemEntity> selectByOrderId(@Param("orderId") Long orderId);
}
