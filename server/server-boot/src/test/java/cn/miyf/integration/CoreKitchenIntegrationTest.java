package cn.miyf.integration;

import cn.miyf.MiyfApplication;
import cn.miyf.service.AuthApplicationService;
import cn.miyf.kitchen.service.KitchenAuthApplicationService;
import cn.miyf.kitchen.service.CategoryApplicationService;
import cn.miyf.kitchen.service.CommentApplicationService;
import cn.miyf.kitchen.service.DishApplicationService;
import cn.miyf.kitchen.service.OrderApplicationService;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.kitchen.bean.model.OrderStatus;
import cn.miyf.bean.dto.AdminLoginDto;
import cn.miyf.kitchen.bean.dto.CommentCreateDto;
import cn.miyf.kitchen.bean.dto.DishSaveDto;
import cn.miyf.kitchen.bean.dto.OrderCreateDto;
import cn.miyf.kitchen.bean.dto.OrderItemCreateDto;
import cn.miyf.kitchen.bean.dto.OrderStatusUpdateDto;
import cn.miyf.bean.dto.WxLoginDto;
import cn.miyf.security.LoginUserContext;
import cn.miyf.support.AbstractIntegrationTest;
import cn.miyf.kitchen.bean.vo.CategoryVo;
import cn.miyf.kitchen.bean.vo.CommentVo;
import cn.miyf.kitchen.bean.vo.DishVo;
import cn.miyf.bean.vo.LoginVo;
import cn.miyf.kitchen.bean.vo.OrderVo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 核心集成测试：登录、RBAC、库存并发、预约流转、评价与评分回算。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:33
 */
@SpringBootTest(classes = MiyfApplication.class)
@ActiveProfiles("test")
class CoreKitchenIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuthApplicationService authApplicationService;
    @Autowired
    private KitchenAuthApplicationService kitchenAuthApplicationService;
    @Autowired
    private CategoryApplicationService categoryApplicationService;
    @Autowired
    private DishApplicationService dishApplicationService;
    @Autowired
    private OrderApplicationService orderApplicationService;
    @Autowired
    private CommentApplicationService commentApplicationService;

    @Test
    void adminAndWxLogin_shouldIssueToken() {
        LoginVo admin = authApplicationService.adminLogin(
                new AdminLoginDto().setUsername("admin").setPassword("change-me"));
        assertNotNull(admin.getToken());
        assertEquals("ADMIN", admin.getPrincipalType());
        assertTrue(admin.getPermissions() != null && !admin.getPermissions().isEmpty());

        BusinessException bad = assertThrows(BusinessException.class,
                () -> authApplicationService.adminLogin(
                        new AdminLoginDto().setUsername("admin").setPassword("wrong")));
        assertEquals(ErrorCode.LOGIN_FAILED.getCode(), bad.getCode());

        LoginVo user = kitchenAuthApplicationService.wxLogin(
                new WxLoginDto().setCode("it-user-login")
                        .setUsername("集成用户")
                        .setPhone("13800138001")
                        .setWechatId("wx_it_user")
                        .setNickname("集成用户"));
        assertNotNull(user.getToken());
        assertEquals("USER", user.getPrincipalType());
        assertNotNull(user.getUserId());
    }

    @Test
    void coreFlow_orderCommentRating_andHideRecalc() {
        LoginVo user1 = kitchenAuthApplicationService.wxLogin(new WxLoginDto().setCode("flow-u1")
                .setUsername("U1").setPhone("13800138002").setWechatId("wx_u1").setNickname("U1"));
        LoginVo user2 = kitchenAuthApplicationService.wxLogin(new WxLoginDto().setCode("flow-u2")
                .setUsername("U2").setPhone("13800138003").setWechatId("wx_u2").setNickname("U2"));
        LoginVo adminLogin = authApplicationService.adminLogin(
                new AdminLoginDto().setUsername("admin").setPassword("change-me"));
        Long adminId = adminLogin.getUserId();

        CategoryVo category = categoryApplicationService.listEnabled().getFirst();
        asOrderAdmin(adminId);
        DishVo dish = dishApplicationService.create(new DishSaveDto()
                .setCategoryId(category.getId())
                .setName("番茄炒蛋-IT")
                .setStockType("LIMITED")
                .setStock(10)
                .setUnit("份")
                .setRecommend(true));
        dish = dishApplicationService.publish(dish.getId());
        assertEquals("ON_SALE", dish.getStatus());

        OrderVo order1 = placeAndComplete(user1.getUserId(), "U1", adminId, dish.getId(), 1);
        asUser(user1.getUserId(), "U1");
        commentApplicationService.create(new CommentCreateDto()
                .setOrderId(order1.getId())
                .setDishId(dish.getId())
                .setRating(5)
                .setContent("好吃"));
        DishVo after5 = dishApplicationService.getAdminDetail(dish.getId());
        assertEquals(0, new BigDecimal("5.00").compareTo(after5.getRating()));
        assertEquals(1, after5.getRatingCount());

        OrderVo order2 = placeAndComplete(user2.getUserId(), "U2", adminId, dish.getId(), 1);
        asUser(user2.getUserId(), "U2");
        CommentVo c4 = commentApplicationService.create(new CommentCreateDto()
                .setOrderId(order2.getId())
                .setDishId(dish.getId())
                .setRating(4)
                .setContent("也不错"));
        DishVo afterAvg = dishApplicationService.getAdminDetail(dish.getId());
        assertEquals(0, new BigDecimal("4.50").compareTo(afterAvg.getRating()));
        assertEquals(2, afterAvg.getRatingCount());

        asOrderAdmin(adminId);
        commentApplicationService.hide(c4.getId());
        DishVo afterHide = dishApplicationService.getAdminDetail(dish.getId());
        assertEquals(0, new BigDecimal("5.00").compareTo(afterHide.getRating()));
        assertEquals(1, afterHide.getRatingCount());
    }

    @Test
    void cancel_shouldRestoreLimitedStock() {
        LoginVo user = kitchenAuthApplicationService.wxLogin(new WxLoginDto().setCode("cancel-u")
                .setUsername("CU").setPhone("13800138004").setWechatId("wx_cu").setNickname("CU"));
        LoginVo adminLogin = authApplicationService.adminLogin(
                new AdminLoginDto().setUsername("admin").setPassword("change-me"));
        CategoryVo category = categoryApplicationService.listEnabled().getFirst();

        asOrderAdmin(adminLogin.getUserId());
        DishVo dish = dishApplicationService.create(new DishSaveDto()
                .setCategoryId(category.getId())
                .setName("回锅肉-取消IT")
                .setStockType("LIMITED")
                .setStock(5)
                .setUnit("份"));
        dishApplicationService.publish(dish.getId());

        asUser(user.getUserId(), "CU");
        OrderVo order = orderApplicationService.create(new OrderCreateDto()
                .setItems(List.of(new OrderItemCreateDto().setDishId(dish.getId()).setQuantity(2))));
        assertEquals(3, dishApplicationService.getAdminDetail(dish.getId()).getStock());

        orderApplicationService.cancelMine(order.getId());
        assertEquals(5, dishApplicationService.getAdminDetail(dish.getId()).getStock());
        assertEquals(OrderStatus.CANCELLED.name(),
                orderApplicationService.getMine(order.getId()).getStatus());
    }

    @Test
    void commentRules_requireCompleted_andRejectDuplicate() {
        LoginVo user = kitchenAuthApplicationService.wxLogin(new WxLoginDto().setCode("cmt-u")
                .setUsername("CM").setPhone("13800138005").setWechatId("wx_cm").setNickname("CM"));
        LoginVo adminLogin = authApplicationService.adminLogin(
                new AdminLoginDto().setUsername("admin").setPassword("change-me"));
        CategoryVo category = categoryApplicationService.listEnabled().getFirst();

        asOrderAdmin(adminLogin.getUserId());
        DishVo dish = dishApplicationService.create(new DishSaveDto()
                .setCategoryId(category.getId())
                .setName("麻婆豆腐-评价IT")
                .setStockType("LIMITED")
                .setStock(5)
                .setUnit("份"));
        dishApplicationService.publish(dish.getId());

        asUser(user.getUserId(), "CM");
        OrderVo pending = orderApplicationService.create(new OrderCreateDto()
                .setItems(List.of(new OrderItemCreateDto().setDishId(dish.getId()).setQuantity(1))));
        BusinessException notReady = assertThrows(BusinessException.class,
                () -> commentApplicationService.create(new CommentCreateDto()
                        .setOrderId(pending.getId())
                        .setDishId(dish.getId())
                        .setRating(5)));
        assertEquals(ErrorCode.ORDER_NOT_COMMENTABLE.getCode(), notReady.getCode());

        OrderVo done = placeAndComplete(user.getUserId(), "CM", adminLogin.getUserId(), dish.getId(), 1);
        asUser(user.getUserId(), "CM");
        commentApplicationService.create(new CommentCreateDto()
                .setOrderId(done.getId())
                .setDishId(dish.getId())
                .setRating(5));
        BusinessException dup = assertThrows(BusinessException.class,
                () -> commentApplicationService.create(new CommentCreateDto()
                        .setOrderId(done.getId())
                        .setDishId(dish.getId())
                        .setRating(4)));
        assertEquals(ErrorCode.DUPLICATE_COMMENT.getCode(), dup.getCode());
    }

    @Test
    void rbac_shouldForbidStatusChangeWithoutPermission() {
        LoginVo user = kitchenAuthApplicationService.wxLogin(new WxLoginDto().setCode("rbac-u")
                .setUsername("RU").setPhone("13800138006").setWechatId("wx_ru").setNickname("RU"));
        LoginVo adminLogin = authApplicationService.adminLogin(
                new AdminLoginDto().setUsername("admin").setPassword("change-me"));
        CategoryVo category = categoryApplicationService.listEnabled().getFirst();

        asOrderAdmin(adminLogin.getUserId());
        DishVo dish = dishApplicationService.create(new DishSaveDto()
                .setCategoryId(category.getId())
                .setName("宫保鸡丁-RBAC")
                .setStockType("LIMITED")
                .setStock(3)
                .setUnit("份"));
        dishApplicationService.publish(dish.getId());

        asUser(user.getUserId(), "RU");
        OrderVo order = orderApplicationService.create(new OrderCreateDto()
                .setItems(List.of(new OrderItemCreateDto().setDishId(dish.getId()).setQuantity(1))));

        asAdmin(adminLogin.getUserId(), "kitchen:order:list");
        BusinessException forbidden = assertThrows(BusinessException.class,
                () -> orderApplicationService.updateStatusAdmin(order.getId(),
                        new OrderStatusUpdateDto().setStatus(OrderStatus.CONFIRMED.name())));
        assertEquals(ErrorCode.FORBIDDEN.getCode(), forbidden.getCode());
    }

    @Test
    void stockConcurrency_shouldNotOversell() throws Exception {
        LoginVo adminLogin = authApplicationService.adminLogin(
                new AdminLoginDto().setUsername("admin").setPassword("change-me"));
        CategoryVo category = categoryApplicationService.listEnabled().getFirst();

        asOrderAdmin(adminLogin.getUserId());
        int stock = 20;
        DishVo dish = dishApplicationService.create(new DishSaveDto()
                .setCategoryId(category.getId())
                .setName("并发测试菜")
                .setStockType("LIMITED")
                .setStock(stock)
                .setUnit("份"));
        dishApplicationService.publish(dish.getId());
        Long dishId = dish.getId();

        int threads = 40;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    LoginVo u = kitchenAuthApplicationService.wxLogin(
                            new WxLoginDto().setCode("conc-" + idx + "-" + System.nanoTime())
                                    .setUsername("C" + idx)
                                    .setPhone(String.format("139%08d", idx))
                                    .setWechatId("wx_c" + idx)
                                    .setNickname("C" + idx));
                    asUser(u.getUserId(), "C" + idx);
                    ready.countDown();
                    start.await(30, TimeUnit.SECONDS);
                    orderApplicationService.create(new OrderCreateDto()
                            .setItems(List.of(new OrderItemCreateDto().setDishId(dishId).setQuantity(1))));
                    success.incrementAndGet();
                } catch (BusinessException ex) {
                    if (ex.getCode() == ErrorCode.STOCK_INSUFFICIENT.getCode()) {
                        failed.incrementAndGet();
                    } else {
                        unexpected.incrementAndGet();
                    }
                } catch (Exception e) {
                    unexpected.incrementAndGet();
                } finally {
                    LoginUserContext.clear();
                }
            });
        }

        assertTrue(ready.await(60, TimeUnit.SECONDS));
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(120, TimeUnit.SECONDS));

        assertEquals(0, unexpected.get(), "unexpected errors in concurrent orders");
        assertEquals(stock, success.get());
        assertEquals(threads - stock, failed.get());
        assertEquals(0, dishApplicationService.getAdminDetail(dishId).getStock());
    }

    private OrderVo placeAndComplete(Long userId, String name, Long adminId, Long dishId, int qty) {
        asUser(userId, name);
        OrderVo order = orderApplicationService.create(new OrderCreateDto()
                .setItems(List.of(new OrderItemCreateDto().setDishId(dishId).setQuantity(qty))));
        asOrderAdmin(adminId);
        orderApplicationService.updateStatusAdmin(order.getId(),
                new OrderStatusUpdateDto().setStatus(OrderStatus.CONFIRMED.name()));
        orderApplicationService.updateStatusAdmin(order.getId(),
                new OrderStatusUpdateDto().setStatus(OrderStatus.PREPARING.name()));
        orderApplicationService.updateStatusAdmin(order.getId(),
                new OrderStatusUpdateDto().setStatus(OrderStatus.READY.name()));
        return orderApplicationService.updateStatusAdmin(order.getId(),
                new OrderStatusUpdateDto().setStatus(OrderStatus.COMPLETED.name()));
    }
}
