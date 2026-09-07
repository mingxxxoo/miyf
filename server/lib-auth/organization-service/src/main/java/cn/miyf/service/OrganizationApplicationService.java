package cn.miyf.service;

import cn.miyf.bean.dto.OrgUnitSaveDto;
import cn.miyf.bean.entity.SysOrgUnitEntity;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.repository.mapper.SysOrgUnitMapper;
import cn.miyf.security.AuthPrincipal;
import cn.miyf.security.DataScope;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
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

    public OrganizationApplicationService(SysOrgUnitMapper orgUnitMapper,
                                          SnowflakeIdGenerator snowflakeIdGenerator,
                                          DataScopeService dataScopeService) {
        this.orgUnitMapper = orgUnitMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.dataScopeService = dataScopeService;
    }

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

    @Transactional
    public SysOrgUnitEntity createOrgUnit(OrgUnitSaveDto dto) {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        Long parentId = parseId(dto.getParentId());
        if (parentId != null) {
            dataScopeService.assertCanAccessOrg(principal, parentId);
        } else if (principal.getDataScope() != DataScope.ALL) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前数据范围不允许创建顶级组织");
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

    @Transactional
    public SysOrgUnitEntity updateOrgUnit(Long id, OrgUnitSaveDto dto) {
        AuthPrincipal principal = dataScopeService.requireAdmin();
        SysOrgUnitEntity entity = requireOrg(id);
        dataScopeService.assertCanAccessOrg(principal, id);
        Long parentId = parseId(dto.getParentId());
        if (parentId != null) {
            dataScopeService.assertCanAccessOrg(principal, parentId);
        } else if (principal.getDataScope() != DataScope.ALL) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前数据范围不允许将组织提升为顶级");
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
