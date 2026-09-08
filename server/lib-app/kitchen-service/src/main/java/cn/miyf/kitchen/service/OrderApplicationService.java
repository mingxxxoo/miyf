package cn.miyf.kitchen.service;

import cn.miyf.auth.security.SecurityUtils;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.PageResult;
import cn.miyf.common.query.QueryConditionHolder;
import cn.miyf.kitchen.bean.dto.OrderCreateDto;
import cn.miyf.kitchen.bean.dto.OrderItemCreateDto;
import cn.miyf.kitchen.bean.dto.OrderStatusUpdateDto;
import cn.miyf.kitchen.bean.model.Dish;
import cn.miyf.kitchen.bean.model.Order;
import cn.miyf.kitchen.bean.model.OrderItem;
import cn.miyf.kitchen.bean.model.OrderStatus;
import cn.miyf.kitchen.bean.model.order.OrderStatusMachine;
import cn.miyf.kitchen.bean.qo.OrderPageQo;
import cn.miyf.kitchen.bean.vo.OrderItemVo;
import cn.miyf.kitchen.bean.vo.OrderVo;
import cn.miyf.kitchen.repository.DishRepository;
import cn.miyf.kitchen.repository.OrderRepository;
import cn.miyf.service.BaseApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 预约应用服务：创建扣库存、状态机流转、取消回补。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:40
 */
@Service
public class OrderApplicationService extends BaseApplicationService {

    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderRepository orderRepository;
    private final DishRepository dishRepository;

    /**
     * 构造预约服务。
     *
     * @param orderRepository 预约仓储
     * @param dishRepository  菜品仓储
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    public OrderApplicationService(OrderRepository orderRepository, DishRepository dishRepository) {
        this.orderRepository = orderRepository;
        this.dishRepository = dishRepository;
    }

    /**
     * 用户创建预约：校验上架 → 原子扣库存 → 写单头与明细。
     *
     * @param dto 请求
     * @return 预约 VO（含「厨房收到啦」提示）
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    @Transactional
    public OrderVo create(OrderCreateDto dto) {
        Long userId = SecurityUtils.currentUserId();
        Map<Long, Integer> quantityByDish = mergeQuantities(dto.getItems());
        Map<Long, String> remarkByDish = firstRemarkByDish(dto.getItems());

        List<OrderItem> items = new ArrayList<>();
        // 先校验并扣减，失败整单回滚
        for (Map.Entry<Long, Integer> entry : quantityByDish.entrySet()) {
            Long dishId = entry.getKey();
            int quantity = entry.getValue();
            Dish dish = requireById(dishRepository, dishId, "菜品不存在");
            requireTrue("ON_SALE".equals(dish.getStatus()), ErrorCode.INVALID_STATUS, "菜品未上架，暂时不能预约");
            if ("LIMITED".equals(dish.getStockType())) {
                requireTrue(dishRepository.deductStock(dishId, quantity),
                        ErrorCode.STOCK_INSUFFICIENT, "「" + dish.getName() + "」可提供份数不足");
            }
            OrderItem item = new OrderItem();
            item.setDishId(dishId);
            item.setDishName(dish.getName());
            item.setQuantity(quantity);
            item.setUnit(dish.getUnit() == null ? "份" : dish.getUnit());
            item.setRemark(remarkByDish.get(dishId));
            items.add(item);
        }

        Order order = new Order();
        order.setOrderNo(nextOrderNo());
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING.name());
        order.setRemark(dto.getRemark());
        order.setItems(items);
        Order saved = orderRepository.insertWithItems(order);
        OrderVo vo = toVo(saved);
        vo.setDisplayTip("厨房收到啦");
        return vo;
    }

    /**
     * 用户端本人预约分页。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    public PageResult<OrderVo> pageMine(OrderPageQo qo) {
        return QueryConditionHolder.run(qo, "create_time DESC", () -> {
            Long userId = SecurityUtils.currentUserId();
            String status = normalizeStatusFilter(qo.getStatus());
            long page = pageOf(qo);
            long pageSize = pageSizeOf(qo);
            PageResult<Order> result = orderRepository.pageByUser(userId, status, page, pageSize);
            return PageResult.of(result.records().stream().map(this::toVo).toList(),
                    result.total(), result.page(), result.pageSize());
        });
    }

    /**
     * 用户端预约详情（仅本人）。
     *
     * @param id 预约 ID
     * @return 详情
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    public OrderVo getMine(Long id) {
        Order order = requireById(orderRepository, id, "预约不存在");
        requireTrue(order.getUserId().equals(SecurityUtils.currentUserId()),
                ErrorCode.FORBIDDEN, "只能查看自己的预约");
        return toVo(order);
    }

    /**
     * 用户取消：仅 PENDING，并回补 LIMITED 库存。
     *
     * @param id 预约 ID
     * @return 取消后预约
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    @Transactional
    public OrderVo cancelMine(Long id) {
        Order order = requireById(orderRepository, id, "预约不存在");
        requireTrue(order.getUserId().equals(SecurityUtils.currentUserId()),
                ErrorCode.FORBIDDEN, "只能取消自己的预约");
        OrderStatus current = OrderStatus.from(order.getStatus());
        requireTrue(OrderStatusMachine.canUserCancel(current),
                ErrorCode.INVALID_STATUS, "当前状态不可取消");
        transitAndRestore(order, OrderStatus.CANCELLED);
        OrderVo vo = toVo(requireById(orderRepository, id, "预约不存在"));
        vo.setDisplayTip("这次预约取消啦");
        return vo;
    }

    /**
     * 管理端分页。
     *
     * @param qo 查询条件
     * @return 分页
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    public PageResult<OrderVo> pageAdmin(OrderPageQo qo) {
        return QueryConditionHolder.run(qo, "create_time DESC", () -> {
            String status = normalizeStatusFilter(qo.getStatus());
            long page = pageOf(qo);
            long pageSize = pageSizeOf(qo);
            PageResult<Order> result = orderRepository.pageAdmin(
                    status, qo.getOrderNo(), qo.getUserId(), page, pageSize);
            return PageResult.of(result.records().stream().map(this::toVo).toList(),
                    result.total(), result.page(), result.pageSize());
        });
    }

    /**
     * 管理端详情。
     *
     * @param id 预约 ID
     * @return 详情
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    public OrderVo getAdmin(Long id) {
        return toVo(requireById(orderRepository, id, "预约不存在"));
    }

    /**
     * 管理端状态流转；取消时回补库存。
     *
     * @param id  预约 ID
     * @param dto 目标状态
     * @return 更新后预约
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    @Transactional
    public OrderVo updateStatusAdmin(Long id, OrderStatusUpdateDto dto) {
        Order order = requireById(orderRepository, id, "预约不存在");
        OrderStatus target = parseStatus(dto.getStatus());
        assertAdminPermissionForTarget(target);
        transitAndRestore(order, target);
        return toVo(requireById(orderRepository, id, "预约不存在"));
    }

    /**
     * 状态流转；若目标为 CANCELLED 则回补 LIMITED 份数。
     *
     * @param order  当前预约（含明细）
     * @param target 目标状态
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    private void transitAndRestore(Order order, OrderStatus target) {
        OrderStatus from = OrderStatus.from(order.getStatus());
        OrderStatusMachine.assertTransit(from, target);
        boolean updated = orderRepository.updateStatus(order.getId(), from.name(), target.name());
        requireTrue(updated, ErrorCode.CONFLICT, "预约状态已变更，请刷新后重试");
        if (target == OrderStatus.CANCELLED) {
            restoreStock(order);
        }
    }

    /**
     * 取消时回补 LIMITED 菜品份数；UNLIMITED 不处理。
     *
     * @param order 预约（含明细）
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    private void restoreStock(Order order) {
        if (order.getItems() == null) {
            return;
        }
        for (OrderItem item : order.getItems()) {
            // restoreStock SQL 仅更新 LIMITED；UNLIMITED 无影响
            dishRepository.restoreStock(item.getDishId(), item.getQuantity());
        }
    }

    /**
     * 管理端按目标状态校验权限码。
     *
     * @param target 目标状态
     * @history 1.00 2026-09-04 17:40 XieMingJie Created.
     */
    private void assertAdminPermissionForTarget(OrderStatus target) {
        var principal = SecurityUtils.requireAdmin();
        String required = switch (target) {
            case CANCELLED -> "kitchen:order:cancel";
            case COMPLETED -> "kitchen:order:complete";
            default -> "kitchen:order:update";
        };
        requireTrue(principal.hasPermission(required), ErrorCode.FORBIDDEN, "没有权限执行该状态变更");
    }

    private Map<Long, Integer> mergeQuantities(List<OrderItemCreateDto> items) {
        Map<Long, Integer> merged = new LinkedHashMap<>();
        for (OrderItemCreateDto item : items) {
            requireTrue(item.getQuantity() != null && item.getQuantity() > 0,
                    ErrorCode.BAD_REQUEST, "预约份数必须大于 0");
            merged.merge(item.getDishId(), item.getQuantity(), Integer::sum);
        }
        requireTrue(!merged.isEmpty(), ErrorCode.BAD_REQUEST, "请至少选择一道菜");
        return merged;
    }

    private Map<Long, String> firstRemarkByDish(List<OrderItemCreateDto> items) {
        Map<Long, String> remarks = new LinkedHashMap<>();
        for (OrderItemCreateDto item : items) {
            remarks.putIfAbsent(item.getDishId(), item.getRemark());
        }
        return remarks;
    }

    private String nextOrderNo() {
        int suffix = ThreadLocalRandom.current().nextInt(10000);
        return "R" + LocalDateTime.now().format(ORDER_NO_TIME) + String.format("%04d", suffix);
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return parseStatus(status).name();
    }

    private OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.from(status.trim().toUpperCase());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "预约状态无效");
        }
    }

    private OrderVo toVo(Order order) {
        Map<Long, String> coverByDishId = resolveCoverImages(order.getItems());
        List<OrderItemVo> items = order.getItems() == null ? List.of() : order.getItems().stream()
                .map(item -> new OrderItemVo()
                        .setId(item.getId())
                        .setDishId(item.getDishId())
                        .setDishName(item.getDishName())
                        .setCoverImage(coverByDishId.get(item.getDishId()))
                        .setQuantity(item.getQuantity())
                        .setUnit(item.getUnit())
                        .setRemark(item.getRemark()))
                .toList();
        return new OrderVo()
                .setId(order.getId())
                .setOrderNo(order.getOrderNo())
                .setUserId(order.getUserId())
                .setStatus(order.getStatus())
                .setRemark(order.getRemark())
                .setCreateTime(order.getCreateTime())
                .setLastModifyTime(order.getLastModifyTime())
                .setItems(items);
    }

    /**
     * 批量解析明细菜品封面，避免列表页 N+1 以外的重复查询浪费。
     *
     * @param items 明细
     * @return dishId → coverImage
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    private Map<Long, String> resolveCoverImages(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return Map.of();
        }
        Set<Long> dishIds = items.stream()
                .map(OrderItem::getDishId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> covers = new LinkedHashMap<>();
        for (Long dishId : dishIds) {
            dishRepository.findById(dishId).ifPresent(dish -> {
                if (dish.getCoverImage() != null && !dish.getCoverImage().isBlank()) {
                    covers.put(dishId, dish.getCoverImage());
                }
            });
        }
        return covers;
    }
}
