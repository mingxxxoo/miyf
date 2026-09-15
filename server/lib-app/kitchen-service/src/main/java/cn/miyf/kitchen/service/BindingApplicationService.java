package cn.miyf.kitchen.service;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.dto.BindingApplyDto;
import cn.miyf.kitchen.bean.dto.BindingRejectDto;
import cn.miyf.kitchen.bean.entity.KitchenBindingEntity;
import cn.miyf.kitchen.bean.entity.KitchenEntity;
import cn.miyf.kitchen.bean.entity.KitchenInviteEntity;
import cn.miyf.kitchen.bean.entity.UserEntity;
import cn.miyf.kitchen.bean.qo.BindingPageQo;
import cn.miyf.kitchen.bean.vo.KitchenBindingVo;
import cn.miyf.kitchen.repository.KitchenBindingRepository;
import cn.miyf.kitchen.repository.KitchenRepository;
import cn.miyf.kitchen.repository.UserRepository;
import cn.miyf.service.BaseApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * 厨师–食客绑定：一客一厨，厨师确认后生效。
 *
 * @author XieMingJie
 * @since 2026-09-15
 * @history 1.00 2026-09-15 XieMingJie Created.
 */
@Service
@RequiredArgsConstructor
public class BindingApplicationService extends BaseApplicationService {

    private static final Set<String> STATUSES = Set.of("PENDING", "BOUND", "REJECTED", "UNBOUND");

    private final KitchenBindingRepository bindingRepository;
    private final KitchenRepository kitchenRepository;
    private final UserRepository userRepository;
    private final KitchenAccessService kitchenAccessService;
    private final InviteApplicationService inviteApplicationService;

    /**
     * 食客当前绑定关系（优先 BOUND，其次 PENDING，再次最新 REJECTED）。
     *
     * @return 绑定 VO，无记录时为 null
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public KitchenBindingVo getMine() {
        UserEntity diner = kitchenAccessService.requireDiner();
        KitchenBindingEntity active = bindingRepository.selectActiveByDiner(diner.getId());
        return active == null ? null : toVo(active);
    }

    /**
     * 凭邀请码/token 申请加入厨房；一客一厨，且同时仅允许一条 PENDING。
     *
     * @param dto 邀请码或 token
     * @return 新建或已存在的 PENDING/BOUND
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public KitchenBindingVo apply(BindingApplyDto dto) {
        UserEntity diner = kitchenAccessService.requireDiner();
        KitchenInviteEntity invite = inviteApplicationService.requireActive(dto.getCode(), dto.getToken());
        KitchenEntity kitchen = kitchenAccessService.requireKitchen(invite.getKitchenId());
        if (kitchen.getOwnerUserId().equals(diner.getId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能加入自己的厨房");
        }
        KitchenBindingEntity currentBound = bindingRepository.selectBoundByDiner(diner.getId());
        if (currentBound != null) {
            // 已绑定同一厨房则幂等返回；绑定其他厨房须先解除
            if (currentBound.getKitchenId().equals(kitchen.getId())) {
                return toVo(currentBound);
            }
            KitchenEntity other = kitchenAccessService.requireKitchen(currentBound.getKitchenId());
            throw new BusinessException(ErrorCode.CONFLICT, "请先解除与「" + other.getName() + "」的绑定");
        }
        KitchenBindingEntity samePending = bindingRepository.selectPending(kitchen.getId(), diner.getId());
        if (samePending != null) {
            return toVo(samePending);
        }
        KitchenBindingEntity otherPending = bindingRepository.selectPendingByDiner(diner.getId());
        if (otherPending != null) {
            KitchenEntity other = kitchenAccessService.requireKitchen(otherPending.getKitchenId());
            throw new BusinessException(ErrorCode.CONFLICT,
                    "已有待确认申请「" + other.getName() + "」，请先取消或等待确认");
        }
        Instant now = Instant.now();
        KitchenBindingEntity entity = new KitchenBindingEntity()
                .setKitchenId(kitchen.getId())
                .setDinerUserId(diner.getId())
                .setStatus("PENDING")
                .setAppliedAt(now);
        try {
            bindingRepository.insert(entity);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // 唯一约束并发：同厨已有关系则返回，否则仍冲突
            KitchenBindingEntity raced = bindingRepository.selectActiveByDiner(diner.getId());
            if (raced != null && raced.getKitchenId().equals(kitchen.getId())) {
                return toVo(raced);
            }
            throw new BusinessException(ErrorCode.CONFLICT, "已有待确认或已绑定的厨房，请先取消或解除");
        }
        inviteApplicationService.markUsed(invite.getId());
        return toVo(entity);
    }

    /**
     * 厨师端：本厨房绑定分页。
     *
     * @param qo 状态与分页
     * @return 分页结果
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public PageResult<KitchenBindingVo> pageChef(BindingPageQo qo) {
        KitchenEntity kitchen = kitchenAccessService.requireOwnedKitchen();
        return pageByKitchen(kitchen.getId(), qo.getStatus(), qo);
    }

    /**
     * 厨师通过待确认申请；若食客已 BOUND 其他厨房则拒绝。
     *
     * @param id 绑定 ID
     * @return 更新后绑定
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public KitchenBindingVo approve(Long id) {
        KitchenBindingEntity entity = requireChefPending(id);
        UserEntity diner = requireById(userRepository, entity.getDinerUserId(), "食客不存在");
        KitchenBindingEntity other = bindingRepository.selectBoundByDiner(diner.getId());
        if (other != null && !other.getId().equals(entity.getId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "该食客已绑定其他厨房");
        }
        entity.setStatus("BOUND");
        entity.setDecidedAt(Instant.now());
        entity.setRejectReason(null);
        bindingRepository.updateById(entity);
        return toVo(entity);
    }

    /**
     * 厨师拒绝待确认申请。
     *
     * @param id  绑定 ID
     * @param dto 拒绝原因（可空）
     * @return 更新后绑定
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public KitchenBindingVo reject(Long id, BindingRejectDto dto) {
        KitchenBindingEntity entity = requireChefPending(id);
        entity.setStatus("REJECTED");
        entity.setDecidedAt(Instant.now());
        entity.setRejectReason(dto == null ? null : dto.getReason());
        bindingRepository.updateById(entity);
        return toVo(entity);
    }

    /**
     * 厨师或食客解除绑定；PENDING 可取消，BOUND 须无进行中预约。
     *
     * @param id 绑定 ID
     * @return 更新后绑定
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public KitchenBindingVo unbind(Long id) {
        KitchenBindingEntity entity = requireById(bindingRepository, id, "绑定不存在");
        UserEntity user = kitchenAccessService.requireUser();
        KitchenEntity kitchen = kitchenAccessService.requireKitchen(entity.getKitchenId());
        boolean owner = kitchen.getOwnerUserId().equals(user.getId());
        boolean diner = entity.getDinerUserId().equals(user.getId());
        requireTrue(owner || diner, ErrorCode.FORBIDDEN, "无权解除该绑定");
        if ("PENDING".equals(entity.getStatus())) {
            // 食客取消申请，或厨师撤回待确认（等同拒绝）
            entity.setStatus("UNBOUND");
            entity.setUnboundAt(Instant.now());
            if (owner && !diner) {
                entity.setDecidedAt(Instant.now());
                entity.setRejectReason("厨师已取消待确认申请");
            }
            bindingRepository.updateById(entity);
            return toVo(entity);
        }
        requireTrue("BOUND".equals(entity.getStatus()), ErrorCode.INVALID_STATUS, "当前状态不可解除");
        if (kitchenAccessService.hasOpenOrders(entity.getKitchenId(), entity.getDinerUserId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "存在进行中的预约，请先完成或取消");
        }
        entity.setStatus("UNBOUND");
        entity.setUnboundAt(Instant.now());
        bindingRepository.updateById(entity);
        return toVo(entity);
    }

    /**
     * 管理端绑定分页。
     *
     * @param qo 厨房、状态与分页
     * @return 分页结果
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public PageResult<KitchenBindingVo> pageAdmin(BindingPageQo qo) {
        String status = normalizeStatus(qo.getStatus());
        long page = pageOf(qo);
        long rows = pageSizeOf(qo);
        List<KitchenBindingVo> records = bindingRepository
                .selectAdminPage(qo.getKitchenId(), status, offset(page, rows), rows)
                .stream()
                .map(this::toVo)
                .toList();
        long total = bindingRepository.countAdminPage(qo.getKitchenId(), status);
        return PageResult.of(records, total, page, rows);
    }

    /**
     * 管理端强制解除 BOUND/PENDING；BOUND 时若有进行中预约则禁止。
     *
     * @param id 绑定 ID
     * @return 更新后绑定
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public KitchenBindingVo unbindAdmin(Long id) {
        KitchenBindingEntity entity = requireById(bindingRepository, id, "绑定不存在");
        requireTrue("BOUND".equals(entity.getStatus()) || "PENDING".equals(entity.getStatus()),
                ErrorCode.INVALID_STATUS, "当前状态不可解除");
        if ("BOUND".equals(entity.getStatus())
                && kitchenAccessService.hasOpenOrders(entity.getKitchenId(), entity.getDinerUserId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "存在进行中的预约，请先处理订单");
        }
        entity.setStatus("UNBOUND");
        entity.setUnboundAt(Instant.now());
        bindingRepository.updateById(entity);
        return toVo(entity);
    }

    private PageResult<KitchenBindingVo> pageByKitchen(Long kitchenId, String status, BindingPageQo qo) {
        String normalized = normalizeStatus(status);
        long page = pageOf(qo);
        long rows = pageSizeOf(qo);
        List<KitchenBindingVo> records = bindingRepository
                .selectChefPage(kitchenId, normalized, offset(page, rows), rows)
                .stream()
                .map(this::toVo)
                .toList();
        long total = bindingRepository.countChefPage(kitchenId, normalized);
        return PageResult.of(records, total, page, rows);
    }

    private KitchenBindingEntity requireChefPending(Long id) {
        KitchenEntity kitchen = kitchenAccessService.requireOwnedKitchen();
        KitchenBindingEntity entity = requireById(bindingRepository, id, "绑定不存在");
        requireTrue(kitchen.getId().equals(entity.getKitchenId()), ErrorCode.FORBIDDEN, "无权处理该申请");
        requireTrue("PENDING".equals(entity.getStatus()), ErrorCode.INVALID_STATUS, "申请已处理");
        return entity;
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        requireTrue(STATUSES.contains(normalized), ErrorCode.BAD_REQUEST, "绑定状态无效");
        return normalized;
    }

    private KitchenBindingVo toVo(KitchenBindingEntity entity) {
        KitchenEntity kitchen = kitchenRepository.selectById(entity.getKitchenId());
        UserEntity diner = userRepository.selectById(entity.getDinerUserId());
        return new KitchenBindingVo()
                .setId(entity.getId())
                .setKitchenId(entity.getKitchenId())
                .setKitchenName(kitchen == null ? null : kitchen.getName())
                .setDinerUserId(entity.getDinerUserId())
                .setDinerNickname(diner == null ? null : diner.getNickname())
                .setStatus(entity.getStatus())
                .setRejectReason(entity.getRejectReason())
                .setAppliedAt(entity.getAppliedAt())
                .setDecidedAt(entity.getDecidedAt())
                .setUnboundAt(entity.getUnboundAt());
    }
}
