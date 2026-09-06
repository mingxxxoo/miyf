package cn.miyf.kitchen.repository;

import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.entity.OrderEntity;
import cn.miyf.kitchen.bean.entity.OrderItemEntity;
import cn.miyf.kitchen.bean.model.Order;
import cn.miyf.kitchen.bean.model.OrderItem;
import cn.miyf.kitchen.helper.EntityConverters;
import cn.miyf.kitchen.repository.mapper.OrderItemMapper;
import cn.miyf.kitchen.repository.mapper.OrderMapper;
import cn.miyf.repository.AbstractMybatisRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 预约单仓储实现：单头 CRUD + 明细装载 + 分页/统计。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:40
 */
@Repository
public class OrderRepositoryImpl extends AbstractMybatisRepository<Order, OrderEntity>
        implements OrderRepository {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    /**
     * 构造预约仓储。
     *
     * @param orderMapper     预约 Mapper
     * @param orderItemMapper 明细 Mapper
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    public OrderRepositoryImpl(OrderMapper orderMapper, OrderItemMapper orderItemMapper) {
        super(orderMapper, e -> EntityConverters.toOrder(e, null), EntityConverters::toOrderEntity);
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Override
    protected Long getDomainId(Order domain) {
        return domain.getId();
    }

    @Override
    protected void setDomainId(Order domain, Long id) {
        domain.setId(id);
    }

    @Override
    protected void setDomainTimestamps(Order domain, Instant createdAt, Instant updatedAt) {
        domain.setCreatedAt(createdAt);
        domain.setUpdatedAt(updatedAt);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    @Override
    public Optional<Order> findById(Long id) {
        return super.findById(id).map(this::enrichItems);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    @Override
    public Order insertWithItems(Order order) {
        Order saved = insert(order);
        Instant now = Instant.now();
        List<OrderItem> items = order.getItems() == null ? List.of() : order.getItems();
        for (OrderItem item : items) {
            OrderItemEntity entity = new OrderItemEntity();
            entity.setOrderId(saved.getId());
            entity.setDishId(item.getDishId());
            entity.setDishName(item.getDishName());
            entity.setQuantity(item.getQuantity());
            entity.setUnit(item.getUnit() == null ? "份" : item.getUnit());
            entity.setRemark(item.getRemark());
            entity.setCreatedAt(now);
            orderItemMapper.insert(entity);
            item.setId(entity.getId());
            item.setOrderId(saved.getId());
            item.setCreatedAt(now);
        }
        saved.setItems(new ArrayList<>(items));
        return saved;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    @Override
    public boolean updateStatus(Long id, String fromStatus, String toStatus) {
        return orderMapper.updateStatus(id, fromStatus, toStatus) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    @Override
    public PageResult<Order> pageByUser(Long userId, String status, long page, long pageSize) {
        long offset = (page - 1) * pageSize;
        List<Order> records = orderMapper.selectUserPage(userId, status, offset, pageSize).stream()
                .map(e -> enrichItems(EntityConverters.toOrder(e, null)))
                .toList();
        long total = orderMapper.countUserPage(userId, status);
        return PageResult.of(records, total, page, pageSize);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    @Override
    public PageResult<Order> pageAdmin(String status, String orderNo, Long userId, long page, long pageSize) {
        long offset = (page - 1) * pageSize;
        List<Order> records = orderMapper.selectAdminPage(status, orderNo, userId, offset, pageSize).stream()
                .map(e -> enrichItems(EntityConverters.toOrder(e, null)))
                .toList();
        long total = orderMapper.countAdminPage(status, orderNo, userId);
        return PageResult.of(records, total, page, pageSize);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    @Override
    public long countByStatusSince(String status, Instant since) {
        return orderMapper.countByStatusSince(status, since);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    @Override
    public long countSince(Instant since) {
        return orderMapper.countSince(since);
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    @Override
    public Map<String, Long> countGroupByStatus() {
        Map<String, Long> result = new HashMap<>();
        for (Map<String, Object> row : orderMapper.countGroupByStatus()) {
            Object status = row.get("status");
            Object cnt = row.get("cnt");
            if (status != null && cnt instanceof Number number) {
                result.put(String.valueOf(status), number.longValue());
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Override
    public List<DailyCount> countDailySince(Instant since) {
        return mapDaily(orderMapper.countDailySince(since));
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Override
    public List<DailyCount> countActiveUsersDailySince(Instant since) {
        return mapDaily(orderMapper.countActiveUsersDailySince(since));
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Override
    public List<NameCount> hotDishesSince(Instant since, int limit) {
        List<NameCount> list = new ArrayList<>();
        for (Map<String, Object> row : orderMapper.hotDishesSince(since, limit)) {
            Object name = row.get("name");
            Object cnt = row.get("cnt");
            if (name != null && cnt instanceof Number number) {
                list.add(new NameCount(String.valueOf(name), number.longValue()));
            }
        }
        return list;
    }

    private static List<DailyCount> mapDaily(List<Map<String, Object>> rows) {
        List<DailyCount> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object label = row.get("day_label");
            Object cnt = row.get("cnt");
            if (label != null && cnt instanceof Number number) {
                list.add(new DailyCount(String.valueOf(label), number.longValue()));
            }
        }
        return list;
    }

    /**
     * 装载预约明细。
     *
     * @param order 预约头
     * @return 含明细的预约
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    private Order enrichItems(Order order) {
        if (order == null) {
            return null;
        }
        List<OrderItemEntity> items = orderItemMapper.selectByOrderId(order.getId());
        order.setItems(items.stream().map(EntityConverters::toOrderItem).toList());
        return order;
    }
}
