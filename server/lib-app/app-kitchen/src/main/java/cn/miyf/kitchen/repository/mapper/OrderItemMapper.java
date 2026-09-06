package cn.miyf.kitchen.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.miyf.kitchen.bean.entity.OrderItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 预约明细 Mapper。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemEntity> {

    /**
     * 按预约单查询明细。
     *
     * @param orderId 预约 ID
     * @return 明细列表
     * @history 1.00 2026-09-04 16:41 XieMingJie Created.
     */
    List<OrderItemEntity> selectByOrderId(@Param("orderId") Long orderId);
}
