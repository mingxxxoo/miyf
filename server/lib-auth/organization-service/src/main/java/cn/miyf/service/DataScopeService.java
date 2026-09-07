package cn.miyf.service;

import cn.miyf.bean.entity.SysOrgUnitEntity;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.repository.mapper.SysOrgUnitMapper;
import cn.miyf.security.AuthPrincipal;
import cn.miyf.security.DataScope;
import cn.miyf.security.SecurityUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
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

    public DataScopeService(SysOrgUnitMapper orgUnitMapper) {
        this.orgUnitMapper = orgUnitMapper;
    }

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

    public void assertCanAccessUser(AuthPrincipal principal, Long userId, Long userOrgUnitId) {
        if (!canAccessUser(principal, userId, userOrgUnitId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该用户（数据范围限制）");
        }
    }

    public void assertCanAccessOrg(AuthPrincipal principal, Long orgUnitId) {
        if (!canAccessOrg(principal, orgUnitId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该组织（数据范围限制）");
        }
    }

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
        Map<Long, List<Long>> children = new HashMap<>();
        for (SysOrgUnitEntity unit : all) {
            if (unit.getParentId() == null) {
                continue;
            }
            children.computeIfAbsent(unit.getParentId(), k -> new ArrayList<>()).add(unit.getId());
        }
        Set<Long> result = new HashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            if (!result.add(id)) {
                continue;
            }
            List<Long> kids = children.get(id);
            if (kids != null) {
                queue.addAll(kids);
            }
        }
        return result;
    }
}
