package cn.miyf.service;

import cn.miyf.bean.dto.OrgUnitSaveDto;
import cn.miyf.bean.entity.SysOrgUnitEntity;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.repository.mapper.SysOrgUnitMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

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

    public OrganizationApplicationService(SysOrgUnitMapper orgUnitMapper,
                                          SnowflakeIdGenerator snowflakeIdGenerator) {
        this.orgUnitMapper = orgUnitMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    /**
     * 组织单位列表。
     *
     * @return 列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     * @history 1.01 2026-09-06 XieMingJie 从 IamApplicationService 拆出。
     */
    public List<SysOrgUnitEntity> listOrgUnits() {
        return orgUnitMapper.selectList(Wrappers.<SysOrgUnitEntity>lambdaQuery()
                .orderByAsc(SysOrgUnitEntity::getSortOrder)
                .orderByAsc(SysOrgUnitEntity::getId));
    }

    /**
     * 创建组织单位。
     *
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public SysOrgUnitEntity createOrgUnit(OrgUnitSaveDto dto) {
        Instant now = Instant.now();
        SysOrgUnitEntity entity = new SysOrgUnitEntity()
                .setParentId(parseId(dto.getParentId()))
                .setCode(dto.getCode())
                .setName(dto.getName())
                .setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder())
                .setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "ENABLED");
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        orgUnitMapper.insert(entity);
        return entity;
    }

    /**
     * 更新组织单位。
     *
     * @param id  ID
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public SysOrgUnitEntity updateOrgUnit(Long id, OrgUnitSaveDto dto) {
        SysOrgUnitEntity entity = requireOrg(id);
        entity.setParentId(parseId(dto.getParentId()));
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        if (dto.getSortOrder() != null) {
            entity.setSortOrder(dto.getSortOrder());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            entity.setStatus(dto.getStatus());
        }
        entity.setUpdatedAt(Instant.now());
        orgUnitMapper.updateById(entity);
        return entity;
    }

    /**
     * 删除组织单位。
     *
     * @param id ID
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public void deleteOrgUnit(Long id) {
        requireOrg(id);
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
