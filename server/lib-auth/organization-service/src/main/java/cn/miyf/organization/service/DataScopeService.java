package cn.miyf.organization.service;

import cn.miyf.auth.security.AuthPrincipal;
import cn.miyf.auth.security.DataScope;
import cn.miyf.auth.security.SecurityUtils;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.tree.TreeRelationIndex;
import cn.miyf.organization.bean.entity.SysOrgUnitEntity;
import cn.miyf.organization.repository.mapper.SysOrgUnitMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 数据范围解析与鉴权辅助。
 *
 * @author XieMingJie
 * @since 2026-09-07
 */
@Service
public class DataScopeService {

    private final SysOrgUnitMapper orgUnitMapper;

    /**
     * 构造数据范围服务。
     *
     * @param orgUnitMapper 组织 Mapper
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public DataScopeService(SysOrgUnitMapper orgUnitMapper) {
        this.orgUnitMapper = orgUnitMapper;
    }

    /**
     * 要求当前主体为管理员。
     *
     * @return 管理员主体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public AuthPrincipal requireAdmin() {
        return SecurityUtils.requireAdmin();
    }

    /**
     * 解析当前管理员可访问的组织 ID。
     * <ul>
     *   <li>ALL → null（不限制）</li>
     *   <li>SELF → 空集（不按组织放行，需配合本人 ID）</li>
     *   <li>ORG / ORG_CHILD → 组织集合；无组织时为空集</li>
     * </ul>
     *
     * @param principal 当前主体
     * @return 可访问组织 ID；ALL 时为 null
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public Set<Long> resolveAllowedOrgIds(AuthPrincipal principal) {
        DataScope scope = principal.getDataScope() == null ? DataScope.ALL : principal.getDataScope();
        if (scope == DataScope.ALL) {
            return null;
        }
        if (scope == DataScope.SELF) {
            return Set.of();
        }
        Long orgId = principal.getOrgUnitId();
        if (orgId == null) {
            return Set.of();
        }
        if (scope == DataScope.ORG) {
            return Set.of(orgId);
        }
        return selfAndDescendants(orgId);
    }

    /**
     * 是否可访问指定组织。
     *
     * @param principal 主体
     * @param orgUnitId 组织 ID
     * @return true 可访问
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public boolean canAccessOrg(AuthPrincipal principal, Long orgUnitId) {
        if (principal.getDataScope() == DataScope.ALL) {
            return true;
        }
        if (orgUnitId == null) {
            return false;
        }
        Set<Long> allowed = resolveAllowedOrgIds(principal);
        return allowed != null && allowed.contains(orgUnitId);
    }

    /**
     * 是否可访问指定用户（SELF 按本人；ORG 系按用户所属组织）。
     *
     * @param principal     主体
     * @param userId        目标用户 ID
     * @param userOrgUnitId 目标用户组织
     * @return true 可访问
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public boolean canAccessUser(AuthPrincipal principal, Long userId, Long userOrgUnitId) {
        DataScope scope = principal.getDataScope() == null ? DataScope.ALL : principal.getDataScope();
        if (scope == DataScope.ALL) {
            return true;
        }
        if (scope == DataScope.SELF) {
            return Objects.equals(principal.getId(), userId);
        }
        Set<Long> allowed = resolveAllowedOrgIds(principal);
        return userOrgUnitId != null && allowed != null && allowed.contains(userOrgUnitId);
    }

    /**
     * 断言可访问用户，否则抛 FORBIDDEN。
     *
     * @param principal     主体
     * @param userId        目标用户
     * @param userOrgUnitId 目标组织
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public void assertCanAccessUser(AuthPrincipal principal, Long userId, Long userOrgUnitId) {
        if (!canAccessUser(principal, userId, userOrgUnitId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该用户（数据范围限制）");
        }
    }

    /**
     * 断言可访问组织，否则抛 FORBIDDEN。
     *
     * @param principal 主体
     * @param orgUnitId 组织 ID
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public void assertCanAccessOrg(AuthPrincipal principal, Long orgUnitId) {
        if (!canAccessOrg(principal, orgUnitId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该组织（数据范围限制）");
        }
    }

    /**
     * 断言可为用户指定组织（SELF 禁止；ORG 系须在范围内）。
     *
     * @param principal 主体
     * @param orgUnitId 拟分配组织
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public void assertCanAssignOrg(AuthPrincipal principal, Long orgUnitId) {
        if (principal.getDataScope() == DataScope.ALL) {
            return;
        }
        if (principal.getDataScope() == DataScope.SELF) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前数据范围不允许指定组织");
        }
        if (orgUnitId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "组织不能为空");
        }
        assertCanAccessOrg(principal, orgUnitId);
    }

    private Set<Long> selfAndDescendants(Long rootId) {
        List<SysOrgUnitEntity> all = orgUnitMapper.selectList(Wrappers.<SysOrgUnitEntity>lambdaQuery()
                .select(SysOrgUnitEntity::getId, SysOrgUnitEntity::getParentId));
        // 无限极组织关系：定制 parentId 字段建索引后 BFS 取自身及子孙
        return TreeRelationIndex.of(all, SysOrgUnitEntity::getId, SysOrgUnitEntity::getParentId)
                .selfAndDescendants(rootId);
    }
}
