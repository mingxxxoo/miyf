package cn.miyf.organization.service;

import cn.miyf.auth.security.AuthPrincipal;
import cn.miyf.auth.security.DataScope;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.common.tree.TreeUtils;
import cn.miyf.organization.bean.dto.OrgUnitSaveDto;
import cn.miyf.organization.bean.entity.SysOrgUnitEntity;
import cn.miyf.organization.bean.vo.SysOrgUnitTreeVo;
import cn.miyf.organization.repository.mapper.SysOrgUnitMapper;
import cn.miyf.service.BaseApplicationService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 组织单位应用服务。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Service
public class OrganizationApplicationService extends BaseApplicationService {

    private final SysOrgUnitMapper orgUnitMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final DataScopeService dataScopeService;

    /**
     * 构造组织应用服务。
     *
     * @param orgUnitMapper          组织 Mapper
     * @param snowflakeIdGenerator   雪花 ID
     * @param dataScopeService       数据范围
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public OrganizationApplicationService(SysOrgUnitMapper orgUnitMapper,
                                          SnowflakeIdGenerator snowflakeIdGenerator,
                                          DataScopeService dataScopeService) {
        this.orgUnitMapper = orgUnitMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.dataScopeService = dataScopeService;
    }

    /**
     * 组织单位列表（按当前管理员数据范围过滤）。
     *
     * @return 组织列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<SysOrgUnitEntity> listOrgUnits() {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        List<SysOrgUnitEntity> all = orgUnitMapper.selectList(Wrappers.<SysOrgUnitEntity>lambdaQuery()
                .orderByAsc(SysOrgUnitEntity::getSortOrder)
                .orderByAsc(SysOrgUnitEntity::getId));
        DataScope scope = principal.getDataScope() == null ? DataScope.ALL : principal.getDataScope();
        if (scope == DataScope.ALL) {
            return all;
        }
        if (scope == DataScope.SELF) {
            Long orgId = principal.getOrgUnitId();
            if (orgId == null) {
                return List.of();
            }
            return all.stream().filter(u -> Objects.equals(u.getId(), orgId)).toList();
        }
        Set<Long> allowed = dataScopeService.resolveAllowedOrgIds(principal);
        if (allowed == null || allowed.isEmpty()) {
            return List.of();
        }
        return all.stream().filter(u -> u.getId() != null && allowed.contains(u.getId())).toList();
    }

    /**
     * 组织单位树（按当前管理员数据范围过滤后组无限极树）。
     *
     * @return 组织树根列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public List<SysOrgUnitTreeVo> listOrgUnitsTree() {
        List<SysOrgUnitEntity> flat = listOrgUnits();
        return TreeUtils.builder(
                        SysOrgUnitTreeVo::getId,
                        SysOrgUnitTreeVo::getParentId,
                        SysOrgUnitTreeVo::getChildren,
                        SysOrgUnitTreeVo::setChildren)
                .rootWhen(pid -> pid == null)
                .orphanAsRoot(true)
                .sortBy(Comparator
                        .comparing(SysOrgUnitTreeVo::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SysOrgUnitTreeVo::getId, Comparator.nullsLast(Long::compareTo)))
                .buildMapped(flat, this::toOrgUnitTreeVo);
    }

    private SysOrgUnitTreeVo toOrgUnitTreeVo(SysOrgUnitEntity entity) {
        return new SysOrgUnitTreeVo()
                .setId(entity.getId())
                .setParentId(entity.getParentId())
                .setCode(entity.getCode())
                .setName(entity.getName())
                .setSortOrder(entity.getSortOrder())
                .setStatus(entity.getStatus())
                .setChildren(new ArrayList<>());
    }

    /**
     * 创建组织单位。
     *
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public SysOrgUnitEntity createOrgUnit(OrgUnitSaveDto dto) {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        Long parentId = parseId(dto.getParentId());
        if (parentId != null) {
            dataScopeService.assertCanAccessOrg(principal, parentId);
        } else if (principal.getDataScope() != DataScope.ALL) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前数据范围不允许将组织单位设为父级");
        }
        Instant now = Instant.now();
        SysOrgUnitEntity entity = new SysOrgUnitEntity()
                .setParentId(parentId)
                .setCode(dto.getCode())
                .setName(dto.getName())
                .setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder())
                .setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "ENABLED");
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreateTime(now);
        entity.setLastModifyTime(now);
        orgUnitMapper.insert(entity);
        return entity;
    }

    /**
     * 更新组织单位。
     *
     * @param id  组织 ID
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public SysOrgUnitEntity updateOrgUnit(Long id, OrgUnitSaveDto dto) {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        SysOrgUnitEntity entity = requireOrg(id);
        dataScopeService.assertCanAccessOrg(principal, id);
        Long parentId = parseId(dto.getParentId());
        if (parentId != null) {
            dataScopeService.assertCanAccessOrg(principal, parentId);
        } else if (principal.getDataScope() != DataScope.ALL) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前数据范围不允许将组织单位设为父级");
        }
        entity.setParentId(parentId);
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        if (dto.getSortOrder() != null) {
            entity.setSortOrder(dto.getSortOrder());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            entity.setStatus(dto.getStatus());
        }
        entity.setLastModifyTime(Instant.now());
        orgUnitMapper.updateById(entity);
        return entity;
    }

    /**
     * 删除组织单位。
     *
     * @param id 组织 ID
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Transactional
    public void deleteOrgUnit(Long id) {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        requireOrg(id);
        dataScopeService.assertCanAccessOrg(principal, id);
        orgUnitMapper.deleteById(id);
    }

    private SysOrgUnitEntity requireOrg(Long id) {
        SysOrgUnitEntity entity = orgUnitMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "组织单位不存在");
        }
        return entity;
    }

    private Long parseId(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "ID 格式无效: " + raw);
        }
    }
}
