package cn.miyf.kitchen.service;

import cn.miyf.auth.security.SecurityUtils;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.kitchen.bean.entity.KitchenBindingEntity;
import cn.miyf.kitchen.bean.entity.KitchenEntity;
import cn.miyf.kitchen.bean.entity.UserEntity;
import cn.miyf.kitchen.bean.model.Dish;
import cn.miyf.kitchen.bean.vo.KitchenVo;
import cn.miyf.kitchen.repository.KitchenBindingRepository;
import cn.miyf.kitchen.repository.KitchenRepository;
import cn.miyf.kitchen.repository.OrderRepository;
import cn.miyf.kitchen.repository.UserRepository;
import cn.miyf.service.BaseApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 胡闹厨房身份与绑定门控。
 * 食客浏览/下单须已 BOUND 且厨房 OPEN；封禁/停业厨房不可用。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
@Service
@RequiredArgsConstructor
public class KitchenAccessService extends BaseApplicationService {

    private final UserRepository userRepository;
    private final KitchenRepository kitchenRepository;
    private final KitchenBindingRepository bindingRepository;
    private final OrderRepository orderRepository;

    /**
     * 当前登录厨房用户。
     *
     * @return 用户实体
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public UserEntity requireUser() {
        return requireById(userRepository, SecurityUtils.currentUserId(), "用户不存在");
    }

    /**
     * 要求已开通厨师身份（chef=true）。
     *
     * @return 厨师用户
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public UserEntity requireChef() {
        UserEntity user = requireUser();
        if (!Boolean.TRUE.equals(user.getChef())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "请先选择厨师身份");
        }
        return user;
    }

    /**
     * 要求已开通食客身份（diner=true）。
     *
     * @return 食客用户
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public UserEntity requireDiner() {
        UserEntity user = requireUser();
        if (!Boolean.TRUE.equals(user.getDiner())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "请先选择食客身份");
        }
        return user;
    }

    /**
     * 厨师建菜拉分类，或已绑定且厨房营业中的食客浏览分类。
     *
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public void requireChefOrBound() {
        UserEntity user = requireUser();
        if (Boolean.TRUE.equals(user.getChef())) {
            return;
        }
        if (Boolean.TRUE.equals(user.getDiner())) {
            KitchenBindingEntity bound = bindingRepository.selectBoundByDiner(user.getId());
            if (bound != null) {
                requireKitchenOpen(requireKitchen(bound.getKitchenId()));
                return;
            }
        }
        throw new BusinessException(ErrorCode.BINDING_REQUIRED, "请先加入厨房");
    }

    /**
     * 当前厨师名下的个人厨房（一用户一厨）。
     *
     * @return 厨房实体
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public KitchenEntity requireOwnedKitchen() {
        UserEntity chef = requireChef();
        KitchenEntity kitchen = kitchenRepository.selectByOwnerUserId(chef.getId());
        if (kitchen == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "请先创建厨房");
        }
        return kitchen;
    }

    /**
     * 按 ID 取厨房，不存在则抛错。
     *
     * @param kitchenId 厨房 ID
     * @return 厨房实体
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public KitchenEntity requireKitchen(Long kitchenId) {
        return requireById(kitchenRepository, kitchenId, "厨房不存在");
    }

    /**
     * 食客当前 BOUND 绑定。
     *
     * @return 绑定实体
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public KitchenBindingEntity requireBound() {
        UserEntity diner = requireDiner();
        KitchenBindingEntity bound = bindingRepository.selectBoundByDiner(diner.getId());
        if (bound == null) {
            throw new BusinessException(ErrorCode.BINDING_REQUIRED, "请先加入厨房");
        }
        return bound;
    }

    /**
     * 食客浏览/下单：须已绑定且厨房营业中。
     *
     * @return 营业中厨房
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public KitchenEntity requireBoundKitchen() {
        KitchenEntity kitchen = requireKitchen(requireBound().getKitchenId());
        requireKitchenOpen(kitchen);
        return kitchen;
    }

    /**
     * 组织协作做菜：当前用户所属厨房，厨房主人与已绑定成员均可使用。
     * <p>
     * 一用户至多一个厨房：优先取本人作为主人的厨房，否则取已绑定厨房；
     * 两者都没有时提示加入厨房。厨房必须处于 OPEN，封禁/停业不可做菜。
     *
     * @return 当前成员厨房
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    public KitchenEntity requireMemberKitchen() {
        UserEntity user = requireUser();
        KitchenEntity kitchen = kitchenRepository.selectByOwnerUserId(user.getId());
        if (kitchen == null) {
            KitchenBindingEntity bound = bindingRepository.selectBoundByDiner(user.getId());
            if (bound == null) {
                throw new BusinessException(ErrorCode.BINDING_REQUIRED, "请先加入厨房");
            }
            kitchen = requireKitchen(bound.getKitchenId());
        }
        requireKitchenOpen(kitchen);
        return kitchen;
    }

    /**
     * 组织协作做菜：当前成员所属组织单元 ID。
     * <p>
     * 组织归属来自厨房的 {@code org_unit_id}；未归属组织的厨房不可使用组织级菜品库/任务池。
     *
     * @return 组织单元 ID
     * @history 1.00 2026-10-31 14:00 XieMingJie Created.
     */
    public Long requireMemberOrgUnitId() {
        return requireOrgUnitId(requireMemberKitchen());
    }

    /**
     * 取厨房所属组织单元 ID，未归属时抛出业务异常。
     *
     * @param kitchen 厨房实体
     * @return 组织单元 ID
     * @history 1.00 2026-10-31 14:00 XieMingJie Created.
     */
    public Long requireOrgUnitId(KitchenEntity kitchen) {
        if (kitchen == null || kitchen.getOrgUnitId() == null) {
            throw new BusinessException(ErrorCode.INVALID_STATUS,
                    "当前厨房未归属组织，请先由管理员配置组织");
        }
        return kitchen.getOrgUnitId();
    }

    /**
     * 校验当前用户属于指定厨房（主人或已绑定成员）。
     *
     * @param kitchenId 厨房 ID
     * @return 所属厨房
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    public KitchenEntity requireMemberOfKitchen(Long kitchenId) {
        KitchenEntity kitchen = requireMemberKitchen();
        if (kitchenId == null || !kitchenId.equals(kitchen.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该厨房的数据");
        }
        return kitchen;
    }

    /**
     * 当前用户是否为指定厨房主人。
     *
     * @param kitchen 厨房实体
     * @return true 表示厨房主人
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    public boolean isOwner(KitchenEntity kitchen) {
        return kitchen != null && SecurityUtils.currentUserId() != null
                && SecurityUtils.currentUserId().equals(kitchen.getOwnerUserId());
    }

    /**
     * 校验食客已绑定指定厨房且该厨 OPEN。
     *
     * @param kitchenId 厨房 ID
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public void requireBoundToKitchen(Long kitchenId) {
        KitchenBindingEntity bound = requireBound();
        if (!bound.getKitchenId().equals(kitchenId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该厨房");
        }
        requireKitchenOpen(requireKitchen(kitchenId));
    }

    /**
     * 校验食客可访问该菜品所属厨房。
     *
     * @param dish 菜品领域对象
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public void requireBoundToDish(Dish dish) {
        if (dish == null || dish.getKitchenId() == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜品不存在");
        }
        requireBoundToKitchen(dish.getKitchenId());
    }

    /**
     * 管理端建菜等：厨房须存在；封禁不可用。
     *
     * @param kitchenId 厨房 ID
     * @return 可归属的厨房
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public KitchenEntity requireAssignableKitchen(Long kitchenId) {
        requireTrue(kitchenId != null, ErrorCode.BAD_REQUEST, "请指定厨房");
        KitchenEntity kitchen = requireKitchen(kitchenId);
        if ("BANNED".equals(kitchen.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "厨房已被封禁，无法归属菜品");
        }
        return kitchen;
    }

    /** BANNED/CLOSED 不可浏览预约；仅 OPEN 放行。 */
    private void requireKitchenOpen(KitchenEntity kitchen) {
        if (kitchen == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "厨房不存在");
        }
        if ("BANNED".equals(kitchen.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "该厨房已被封禁，暂时无法使用");
        }
        if ("CLOSED".equals(kitchen.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "该厨房已停业，暂时无法浏览或预约");
        }
        if (!"OPEN".equals(kitchen.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "该厨房暂不可用");
        }
    }

    /**
     * 是否存在进行中预约（解绑门禁）。
     *
     * @param kitchenId   厨房 ID
     * @param dinerUserId 食客用户 ID
     * @return true 表示有未完结单
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public boolean hasOpenOrders(Long kitchenId, Long dinerUserId) {
        return orderRepository.countOpenByKitchenAndUser(kitchenId, dinerUserId) > 0;
    }

    /**
     * 厨房实体转展示 VO。
     *
     * @param entity 厨房实体
     * @return VO，entity 为 null 时返回 null
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public KitchenVo toKitchenVo(KitchenEntity entity) {
        if (entity == null) {
            return null;
        }
        return new KitchenVo()
                .setId(entity.getId())
                .setOwnerUserId(entity.getOwnerUserId())
                .setOrgUnitId(entity.getOrgUnitId())
                .setName(entity.getName())
                .setIntro(entity.getIntro())
                .setCoverImage(entity.getCoverImage())
                .setStatus(entity.getStatus())
                .setCreateTime(entity.getCreateTime());
    }
}
