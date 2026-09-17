package cn.miyf.kitchen.service;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.dto.KitchenSaveDto;
import cn.miyf.kitchen.bean.entity.KitchenEntity;
import cn.miyf.kitchen.bean.entity.UserEntity;
import cn.miyf.kitchen.bean.qo.KitchenPageQo;
import cn.miyf.kitchen.bean.vo.KitchenVo;
import cn.miyf.kitchen.repository.KitchenRepository;
import cn.miyf.service.BaseApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * 厨师个人厨房：一用户一厨。
 * 厨师仅可 OPEN/CLOSED；管理端可额外 BANNED。
 * 创建/保存时默认将厨主以 BOUND 加入本厨圈。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
@Service
@RequiredArgsConstructor
public class KitchenApplicationService extends BaseApplicationService {

    private static final Set<String> CHEF_STATUS = Set.of("OPEN", "CLOSED");
    private static final Set<String> ADMIN_STATUS = Set.of("OPEN", "CLOSED", "BANNED");

    private final KitchenRepository kitchenRepository;
    private final KitchenAccessService kitchenAccessService;
    private final BindingApplicationService bindingApplicationService;

    /**
     * 当前厨师的厨房；尚未创建时返回 null。
     * 已有厨房时补齐厨主默认自绑。
     *
     * @return 厨房 VO
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public KitchenVo getMine() {
        KitchenEntity kitchen = kitchenRepository.selectByOwnerUserId(kitchenAccessService.requireChef().getId());
        if (kitchen != null) {
            bindingApplicationService.ensureOwnerSelfBinding(kitchen);
        }
        return kitchenAccessService.toKitchenVo(kitchen);
    }

    /**
     * 创建或更新本厨资料；新建默认 OPEN，已封禁不可由厨师改状态。
     * 新建或保存后确保厨主以 BOUND 加入本厨圈。
     *
     * @param dto 名称/简介/封面/状态
     * @return 保存后厨房
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public KitchenVo saveMine(KitchenSaveDto dto) {
        UserEntity chef = kitchenAccessService.requireChef();
        KitchenEntity existing = kitchenRepository.selectByOwnerUserId(chef.getId());
        String name = dto.getName().trim();
        if (existing == null) {
            KitchenEntity created = new KitchenEntity()
                    .setOwnerUserId(chef.getId())
                    .setName(name)
                    .setIntro(dto.getIntro())
                    .setCoverImage(dto.getCoverImage())
                    .setStatus("OPEN");
            kitchenRepository.insert(created);
            bindingApplicationService.ensureOwnerSelfBinding(created);
            return kitchenAccessService.toKitchenVo(created);
        }
        existing.setName(name);
        existing.setIntro(dto.getIntro());
        existing.setCoverImage(dto.getCoverImage());
        if (StringUtils.hasText(dto.getStatus())) {
            String status = dto.getStatus().trim().toUpperCase();
            requireTrue(CHEF_STATUS.contains(status), ErrorCode.BAD_REQUEST, "厨房状态无效");
            if ("BANNED".equals(existing.getStatus())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "厨房已被封禁");
            }
            existing.setStatus(status);
        }
        kitchenRepository.updateById(existing);
        bindingApplicationService.ensureOwnerSelfBinding(existing);
        return kitchenAccessService.toKitchenVo(existing);
    }

    /**
     * 管理端厨房分页。
     *
     * @param qo 关键字、状态与分页
     * @return 分页结果
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    public PageResult<KitchenVo> pageAdmin(KitchenPageQo qo) {
        long page = pageOf(qo);
        long rows = pageSizeOf(qo);
        var records = kitchenRepository.selectAdminPage(qo.getKeyword(), qo.getStatus(), offset(page, rows), rows)
                .stream()
                .map(kitchenAccessService::toKitchenVo)
                .toList();
        long total = kitchenRepository.countAdminPage(qo.getKeyword(), qo.getStatus());
        return PageResult.of(records, total, page, rows);
    }

    /**
     * 管理端更新厨房状态（含封禁）。
     *
     * @param id     厨房 ID
     * @param status OPEN/CLOSED/BANNED
     * @return 更新后厨房
     * @history 1.00 2026-09-15 XieMingJie Created.
     */
    @Transactional
    public KitchenVo updateAdminStatus(Long id, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        requireTrue(ADMIN_STATUS.contains(normalized), ErrorCode.BAD_REQUEST, "厨房状态无效");
        KitchenEntity kitchen = requireById(kitchenRepository, id, "厨房不存在");
        kitchen.setStatus(normalized);
        kitchenRepository.updateById(kitchen);
        return kitchenAccessService.toKitchenVo(kitchen);
    }
}
