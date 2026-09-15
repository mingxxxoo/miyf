package cn.miyf.kitchen.service;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.kitchen.bean.entity.KitchenEntity;
import cn.miyf.kitchen.bean.entity.KitchenInviteEntity;
import cn.miyf.kitchen.bean.vo.KitchenInviteVo;
import cn.miyf.kitchen.repository.KitchenInviteRepository;
import cn.miyf.service.BaseApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * 厨房邀请码：生成、换码、按码/token 解析。
 * 仅 OPEN 厨房可被加入；token 优先于短码。
 *
 * @author XieMingJie
 * @since 2026-09-15
 * @history 1.00 2026-09-15 XieMingJie Created.
 */
@Service
@RequiredArgsConstructor
public class InviteApplicationService extends BaseApplicationService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final KitchenInviteRepository inviteRepository;
    private final KitchenAccessService kitchenAccessService;

    /**
     * 当前厨房有效邀请码；无则自动生成。
     *
     * @return 邀请 VO（含小程序 joinPath）
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public KitchenInviteVo getMine() {
        KitchenEntity kitchen = kitchenAccessService.requireOwnedKitchen();
        KitchenInviteEntity invite = inviteRepository.selectActiveByKitchenId(kitchen.getId());
        if (invite == null) {
            invite = createActive(kitchen.getId());
        }
        return toVo(invite);
    }

    /**
     * 作废当前 ACTIVE 邀请并生成新码（换码后旧码立即失效）。
     *
     * @return 新邀请 VO
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public KitchenInviteVo rotateMine() {
        KitchenEntity kitchen = kitchenAccessService.requireOwnedKitchen();
        KitchenInviteEntity current = inviteRepository.selectActiveByKitchenId(kitchen.getId());
        if (current != null) {
            current.setStatus("REVOKED");
            inviteRepository.updateById(current);
        }
        return toVo(createActive(kitchen.getId()));
    }

    /**
     * 解析并校验可用邀请（状态、过期、次数、厨房 OPEN）。
     *
     * @param code  短码（可空）
     * @param token 长 token（优先）
     * @return 有效邀请实体
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public KitchenInviteEntity requireActive(String code, String token) {
        KitchenInviteEntity invite = null;
        if (token != null && !token.isBlank()) {
            invite = inviteRepository.selectByToken(token.trim());
        } else if (code != null && !code.isBlank()) {
            invite = inviteRepository.selectByCode(code.trim().toUpperCase());
        }
        if (invite == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "邀请码无效");
        }
        if (!"ACTIVE".equals(invite.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "邀请码已失效");
        }
        if (invite.getExpireAt() != null && invite.getExpireAt().isBefore(java.time.Instant.now())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "邀请码已过期");
        }
        if (invite.getMaxUses() != null && invite.getUsedCount() != null
                && invite.getUsedCount() >= invite.getMaxUses()) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "邀请码已达使用上限");
        }
        KitchenEntity kitchen = kitchenAccessService.requireKitchen(invite.getKitchenId());
        if (!"OPEN".equals(kitchen.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_STATUS, "该厨房暂不可加入");
        }
        return invite;
    }

    /**
     * 申请成功后累加使用次数。
     *
     * @param inviteId 邀请 ID
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public void markUsed(Long inviteId) {
        inviteRepository.incrementUsedCount(inviteId);
    }

    private KitchenInviteEntity createActive(Long kitchenId) {
        KitchenInviteEntity entity = new KitchenInviteEntity()
                .setKitchenId(kitchenId)
                .setCode(nextCode())
                .setToken(UUID.randomUUID().toString().replace("-", ""))
                .setUsedCount(0)
                .setStatus("ACTIVE");
        inviteRepository.insert(entity);
        return entity;
    }

    /** 生成 8 位易读短码；排除易混字符，冲突最多重试 8 次。 */
    private String nextCode() {
        for (int attempt = 0; attempt < 8; attempt++) {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            String code = sb.toString();
            if (inviteRepository.selectByCode(code) == null) {
                return code;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "邀请码生成失败");
    }

    private KitchenInviteVo toVo(KitchenInviteEntity entity) {
        return new KitchenInviteVo()
                .setId(entity.getId())
                .setKitchenId(entity.getKitchenId())
                .setCode(entity.getCode())
                .setToken(entity.getToken())
                .setJoinPath("/pages/join/index?token=" + entity.getToken())
                .setExpireAt(entity.getExpireAt())
                .setMaxUses(entity.getMaxUses())
                .setUsedCount(entity.getUsedCount())
                .setStatus(entity.getStatus());
    }
}
