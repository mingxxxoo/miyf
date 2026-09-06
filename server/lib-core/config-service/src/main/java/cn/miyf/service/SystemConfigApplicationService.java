package cn.miyf.service;

import cn.miyf.bean.dto.SysConfigSaveDto;
import cn.miyf.bean.entity.SysConfigEntity;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.repository.mapper.SysConfigMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 系统配置应用服务。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Service
public class SystemConfigApplicationService {

    private static final Set<String> VALUE_TYPES = Set.of("STRING", "NUMBER", "BOOLEAN", "JSON");
    private static final Set<String> STATUSES = Set.of("ENABLED", "DISABLED");

    private final SysConfigMapper configMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public SystemConfigApplicationService(SysConfigMapper configMapper,
                                          SnowflakeIdGenerator snowflakeIdGenerator) {
        this.configMapper = configMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    /**
     * 配置列表。
     *
     * @param groupCode 可选分组
     * @param keyword   可选关键字（键/名称）
     * @return 列表
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public List<SysConfigEntity> list(String groupCode, String keyword) {
        return configMapper.selectList(Wrappers.<SysConfigEntity>lambdaQuery()
                .eq(StringUtils.hasText(groupCode), SysConfigEntity::getGroupCode, groupCode)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysConfigEntity::getConfigKey, keyword)
                        .or()
                        .like(SysConfigEntity::getName, keyword))
                .orderByAsc(SysConfigEntity::getGroupCode)
                .orderByAsc(SysConfigEntity::getSortOrder)
                .orderByAsc(SysConfigEntity::getConfigKey));
    }

    /**
     * 按键查询。
     *
     * @param configKey 配置键
     * @return 实体
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public SysConfigEntity getByKey(String configKey) {
        SysConfigEntity entity = configMapper.selectOne(Wrappers.<SysConfigEntity>lambdaQuery()
                .eq(SysConfigEntity::getConfigKey, configKey)
                .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "配置不存在");
        }
        return entity;
    }

    /**
     * 创建配置。
     *
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Transactional
    public SysConfigEntity create(SysConfigSaveDto dto) {
        assertKeyUnique(dto.getConfigKey(), null);
        Instant now = Instant.now();
        SysConfigEntity entity = mapDto(new SysConfigEntity(), dto);
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        configMapper.insert(entity);
        return entity;
    }

    /**
     * 更新配置。
     *
     * @param id  ID
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Transactional
    public SysConfigEntity update(Long id, SysConfigSaveDto dto) {
        SysConfigEntity entity = require(id);
        assertKeyUnique(dto.getConfigKey(), id);
        mapDto(entity, dto);
        entity.setUpdatedAt(Instant.now());
        configMapper.updateById(entity);
        return entity;
    }

    /**
     * 删除配置。
     *
     * @param id ID
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Transactional
    public void delete(Long id) {
        require(id);
        configMapper.deleteById(id);
    }

    private SysConfigEntity require(Long id) {
        SysConfigEntity entity = configMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "配置不存在");
        }
        return entity;
    }

    private void assertKeyUnique(String configKey, Long excludeId) {
        Long cnt = configMapper.selectCount(Wrappers.<SysConfigEntity>lambdaQuery()
                .eq(SysConfigEntity::getConfigKey, configKey)
                .ne(excludeId != null, SysConfigEntity::getId, excludeId));
        if (cnt != null && cnt > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "配置键已存在");
        }
    }

    private SysConfigEntity mapDto(SysConfigEntity entity, SysConfigSaveDto dto) {
        String valueType = normalizeValueType(dto.getValueType());
        String status = normalizeStatus(dto.getStatus());
        String group = StringUtils.hasText(dto.getGroupCode()) ? dto.getGroupCode().trim() : "default";
        return entity
                .setConfigKey(dto.getConfigKey().trim())
                .setConfigValue(dto.getConfigValue())
                .setValueType(valueType)
                .setGroupCode(group)
                .setName(dto.getName().trim())
                .setDescription(dto.getDescription())
                .setStatus(status)
                .setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
    }

    private String normalizeValueType(String raw) {
        String v = StringUtils.hasText(raw) ? raw.trim().toUpperCase(Locale.ROOT) : "STRING";
        if (!VALUE_TYPES.contains(v)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的值类型");
        }
        return v;
    }

    private String normalizeStatus(String raw) {
        String v = StringUtils.hasText(raw) ? raw.trim().toUpperCase(Locale.ROOT) : "ENABLED";
        if (!STATUSES.contains(v)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的状态");
        }
        return v;
    }
}
