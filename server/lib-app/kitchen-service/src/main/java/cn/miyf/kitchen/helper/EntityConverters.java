package cn.miyf.kitchen.helper;

import cn.miyf.kitchen.bean.entity.CategoryEntity;
import cn.miyf.kitchen.bean.entity.CommentEntity;
import cn.miyf.kitchen.bean.entity.DishEntity;
import cn.miyf.kitchen.bean.entity.OperationLogEntity;
import cn.miyf.kitchen.bean.entity.OrderEntity;
import cn.miyf.kitchen.bean.entity.OrderItemEntity;
import cn.miyf.kitchen.bean.entity.RecipeEntity;
import cn.miyf.kitchen.bean.entity.UserEntity;
import cn.miyf.kitchen.bean.model.Category;
import cn.miyf.kitchen.bean.model.Comment;
import cn.miyf.kitchen.bean.model.Dish;
import cn.miyf.kitchen.bean.model.OperationLog;
import cn.miyf.kitchen.bean.model.Order;
import cn.miyf.kitchen.bean.model.OrderItem;
import cn.miyf.kitchen.bean.model.Recipe;
import cn.miyf.kitchen.bean.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Entity ↔ Domain 转换器（厨房业务）。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
public final class EntityConverters {
    private EntityConverters() {
    }

    public static User toUser(UserEntity e) {
        if (e == null) return null;
        User u = new User();
        u.setId(e.getId());
        u.setOpenid(e.getOpenid());
        u.setUnionid(e.getUnionid());
        u.setNickname(e.getNickname());
        u.setAvatarUrl(e.getAvatarUrl());
        u.setStatus(e.getStatus());
        u.setCreatedAt(e.getCreatedAt());
        u.setUpdatedAt(e.getUpdatedAt());
        return u;
    }

    public static UserEntity toUserEntity(User u) {
        UserEntity e = new UserEntity();
        e.setId(u.getId());
        e.setOpenid(u.getOpenid());
        e.setUnionid(u.getUnionid());
        e.setNickname(u.getNickname());
        e.setAvatarUrl(u.getAvatarUrl());
        e.setStatus(u.getStatus());
        e.setCreatedAt(u.getCreatedAt());
        e.setUpdatedAt(u.getUpdatedAt());
        return e;
    }

    public static Category toCategory(CategoryEntity e) {
        if (e == null) return null;
        Category c = new Category();
        c.setId(e.getId());
        c.setName(e.getName());
        c.setIcon(e.getIcon());
        c.setSortOrder(e.getSortOrder() == null ? 0 : e.getSortOrder());
        c.setStatus(e.getStatus());
        c.setCreatedAt(e.getCreatedAt());
        c.setUpdatedAt(e.getUpdatedAt());
        return c;
    }

    public static CategoryEntity toCategoryEntity(Category c) {
        CategoryEntity e = new CategoryEntity();
        e.setId(c.getId());
        e.setName(c.getName());
        e.setIcon(c.getIcon());
        e.setSortOrder(c.getSortOrder());
        e.setStatus(c.getStatus());
        e.setCreatedAt(c.getCreatedAt());
        e.setUpdatedAt(c.getUpdatedAt());
        return e;
    }

    public static Dish toDish(DishEntity e) {
        if (e == null) return null;
        Dish d = new Dish();
        d.setId(e.getId());
        d.setCategoryId(e.getCategoryId());
        d.setName(e.getName());
        d.setSubtitle(e.getSubtitle());
        d.setDescription(e.getDescription());
        d.setCoverImage(e.getCoverImage());
        d.setStatus(e.getStatus());
        d.setSortOrder(e.getSortOrder() == null ? 0 : e.getSortOrder());
        d.setRecommend(Boolean.TRUE.equals(e.getRecommend()));
        d.setStock(e.getStock() == null ? 0 : e.getStock());
        d.setStockType(e.getStockType());
        d.setUnit(e.getUnit());
        d.setRating(e.getRating());
        d.setRatingCount(e.getRatingCount() == null ? 0 : e.getRatingCount());
        d.setCreatedBy(e.getCreatedBy());
        d.setUpdatedBy(e.getUpdatedBy());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }

    public static DishEntity toDishEntity(Dish d) {
        DishEntity e = new DishEntity();
        e.setId(d.getId());
        e.setCategoryId(d.getCategoryId());
        e.setName(d.getName());
        e.setSubtitle(d.getSubtitle());
        e.setDescription(d.getDescription());
        e.setCoverImage(d.getCoverImage());
        e.setStatus(d.getStatus());
        e.setSortOrder(d.getSortOrder());
        e.setRecommend(d.isRecommend());
        e.setStock(d.getStock());
        e.setStockType(d.getStockType());
        e.setUnit(d.getUnit());
        e.setRating(d.getRating());
        e.setRatingCount(d.getRatingCount());
        e.setCreatedBy(d.getCreatedBy());
        e.setUpdatedBy(d.getUpdatedBy());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }

    public static Recipe toRecipe(RecipeEntity e) {
        if (e == null) return null;
        Recipe r = new Recipe();
        r.setId(e.getId());
        r.setDishId(e.getDishId());
        r.setDescription(e.getDescription());
        r.setDifficulty(e.getDifficulty());
        r.setPrepareMinutes(e.getPrepareMinutes());
        r.setCookMinutes(e.getCookMinutes());
        r.setServings(e.getServings());
        r.setIngredients(e.getIngredients());
        r.setSeasonings(e.getSeasonings());
        r.setSteps(e.getSteps());
        r.setTips(e.getTips());
        r.setNutrition(e.getNutrition());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        return r;
    }

    public static RecipeEntity toRecipeEntity(Recipe r) {
        RecipeEntity e = new RecipeEntity();
        e.setId(r.getId());
        e.setDishId(r.getDishId());
        e.setDescription(r.getDescription());
        e.setDifficulty(r.getDifficulty());
        e.setPrepareMinutes(r.getPrepareMinutes());
        e.setCookMinutes(r.getCookMinutes());
        e.setServings(r.getServings());
        e.setIngredients(r.getIngredients());
        e.setSeasonings(r.getSeasonings());
        e.setSteps(r.getSteps());
        e.setTips(r.getTips());
        e.setNutrition(r.getNutrition());
        e.setCreatedAt(r.getCreatedAt());
        e.setUpdatedAt(r.getUpdatedAt());
        return e;
    }

    public static Order toOrder(OrderEntity e, List<OrderItemEntity> items) {
        if (e == null) return null;
        Order o = new Order();
        o.setId(e.getId());
        o.setOrderNo(e.getOrderNo());
        o.setUserId(e.getUserId());
        o.setStatus(e.getStatus());
        o.setRemark(e.getRemark());
        o.setCreatedAt(e.getCreatedAt());
        o.setUpdatedAt(e.getUpdatedAt());
        if (items != null) {
            o.setItems(items.stream().map(EntityConverters::toOrderItem).collect(Collectors.toCollection(ArrayList::new)));
        }
        return o;
    }

    public static OrderItem toOrderItem(OrderItemEntity e) {
        OrderItem i = new OrderItem();
        i.setId(e.getId());
        i.setOrderId(e.getOrderId());
        i.setDishId(e.getDishId());
        i.setDishName(e.getDishName());
        i.setQuantity(e.getQuantity());
        i.setUnit(e.getUnit());
        i.setRemark(e.getRemark());
        i.setCreatedAt(e.getCreatedAt());
        return i;
    }

    public static OrderEntity toOrderEntity(Order o) {
        OrderEntity e = new OrderEntity();
        e.setId(o.getId());
        e.setOrderNo(o.getOrderNo());
        e.setUserId(o.getUserId());
        e.setStatus(o.getStatus());
        e.setRemark(o.getRemark());
        e.setCreatedAt(o.getCreatedAt());
        e.setUpdatedAt(o.getUpdatedAt());
        return e;
    }

    public static Comment toComment(CommentEntity e) {
        if (e == null) return null;
        Comment c = new Comment();
        c.setId(e.getId());
        c.setUserId(e.getUserId());
        c.setDishId(e.getDishId());
        c.setOrderId(e.getOrderId());
        c.setRating(e.getRating());
        c.setContent(e.getContent());
        c.setStatus(e.getStatus());
        c.setCreatedAt(e.getCreatedAt());
        c.setUpdatedAt(e.getUpdatedAt());
        return c;
    }

    public static CommentEntity toCommentEntity(Comment c) {
        CommentEntity e = new CommentEntity();
        e.setId(c.getId());
        e.setUserId(c.getUserId());
        e.setDishId(c.getDishId());
        e.setOrderId(c.getOrderId());
        e.setRating(c.getRating());
        e.setContent(c.getContent());
        e.setStatus(c.getStatus());
        e.setCreatedAt(c.getCreatedAt());
        e.setUpdatedAt(c.getUpdatedAt());
        return e;
    }

    public static OperationLog toLog(OperationLogEntity e) {
        if (e == null) return null;
        OperationLog l = new OperationLog();
        l.setId(e.getId());
        l.setOperatorId(e.getOperatorId());
        l.setOperatorName(e.getOperatorName());
        l.setOperationType(e.getOperationType());
        l.setTargetType(e.getTargetType());
        l.setTargetId(e.getTargetId());
        l.setRequestIp(e.getRequestIp());
        l.setRequestMethod(e.getRequestMethod());
        l.setRequestUri(e.getRequestUri());
        l.setOperationDetail(e.getOperationDetail());
        l.setCreatedAt(e.getCreatedAt());
        return l;
    }

    public static OperationLogEntity toLogEntity(OperationLog l) {
        if (l == null) return null;
        OperationLogEntity e = new OperationLogEntity();
        e.setId(l.getId());
        e.setOperatorId(l.getOperatorId());
        e.setOperatorName(l.getOperatorName());
        e.setOperationType(l.getOperationType());
        e.setTargetType(l.getTargetType());
        e.setTargetId(l.getTargetId());
        e.setRequestIp(l.getRequestIp());
        e.setRequestMethod(l.getRequestMethod());
        e.setRequestUri(l.getRequestUri());
        e.setOperationDetail(l.getOperationDetail());
        e.setCreatedAt(l.getCreatedAt());
        return e;
    }
}
