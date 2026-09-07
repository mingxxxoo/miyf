package cn.miyf.service;

import cn.miyf.bean.dto.SysDictItemSaveDto;
import cn.miyf.bean.dto.SysDictTypeSaveDto;
import cn.miyf.bean.entity.SysDictItemEntity;
import cn.miyf.bean.entity.SysDictTypeEntity;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.repository.mapper.SysDictItemMapper;
import cn.miyf.repository.mapper.SysDictTypeMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 数据字典应用服务。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Service
public class SystemDictApplicationService {

    private static final Set<String> STATUSES = Set.of("ENABLED", "DISABLED");

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictItemMapper dictItemMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public SystemDictApplicationService(SysDictTypeMapper dictTypeMapper,
                                        SysDictItemMapper dictItemMapper,
                                        SnowflakeIdGenerator snowflakeIdGenerator) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictItemMapper = dictItemMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    /**
     * 字典类型列表。
     *
     * @param keyword 可选关键字
     * @return 列表
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public List<SysDictTypeEntity> listTypes(String keyword) {
        return dictTypeMapper.selectList(Wrappers.<SysDictTypeEntity>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysDictTypeEntity::getCode, keyword)
                        .or()
                        .like(SysDictTypeEntity::getName, keyword))
                .orderByAsc(SysDictTypeEntity::getSortOrder)
                .orderByAsc(SysDictTypeEntity::getCode));
    }

    /**
     * 创建字典类型。
     *
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Transactional
    public SysDictTypeEntity createType(SysDictTypeSaveDto dto) {
        assertTypeCodeUnique(dto.getCode(), null);
        Instant now = Instant.now();
        SysDictTypeEntity entity = mapType(new SysDictTypeEntity(), dto);
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreateTime(now);
        entity.setLastModifyTime(now);
        dictTypeMapper.insert(entity);
        return entity;
    }

    /**
     * 更新字典类型。
     *
     * @param id  ID
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Transactional
    public SysDictTypeEntity updateType(Long id, SysDictTypeSaveDto dto) {
        SysDictTypeEntity entity = requireType(id);
        assertTypeCodeUnique(dto.getCode(), id);
        mapType(entity, dto);
        entity.setLastModifyTime(Instant.now());
        dictTypeMapper.updateById(entity);
        return entity;
    }

    /**
     * 删除字典类型（级联删除字典项）。
     *
     * @param id ID
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Transactional
    public void deleteType(Long id) {
        requireType(id);
        dictItemMapper.delete(Wrappers.<SysDictItemEntity>lambdaQuery()
                .eq(SysDictItemEntity::getTypeId, id));
        dictTypeMapper.deleteById(id);
    }

    /**
     * 按类型 ID 列出字典项。
     *
     * @param typeId 类型 ID
     * @return 列表
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public List<SysDictItemEntity> listItemsByTypeId(Long typeId) {
        requireType(typeId);
        return dictItemMapper.selectList(Wrappers.<SysDictItemEntity>lambdaQuery()
                .eq(SysDictItemEntity::getTypeId, typeId)
                .orderByAsc(SysDictItemEntity::getSortOrder)
                .orderByAsc(SysDictItemEntity::getItemValue));
    }

    /**
     * 按类型编码列出字典项（仅启用类型；可含禁用项由调用方过滤）。
     *
     * @param typeCode    类型编码
     * @param enabledOnly 是否仅启用项
     * @return 列表
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public List<SysDictItemEntity> listItemsByTypeCode(String typeCode, boolean enabledOnly) {
        SysDictTypeEntity type = dictTypeMapper.selectOne(Wrappers.<SysDictTypeEntity>lambdaQuery()
                .eq(SysDictTypeEntity::getCode, typeCode)
                .last("LIMIT 1"));
        if (type == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字典类型不存在");
        }
        return dictItemMapper.selectList(Wrappers.<SysDictItemEntity>lambdaQuery()
                .eq(SysDictItemEntity::getTypeId, type.getId())
                .eq(enabledOnly, SysDictItemEntity::getStatus, "ENABLED")
                .orderByAsc(SysDictItemEntity::getSortOrder)
                .orderByAsc(SysDictItemEntity::getItemValue));
    }

    /**
     * 创建字典项。
     *
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Transactional
    public SysDictItemEntity createItem(SysDictItemSaveDto dto) {
        Long typeId = parseRequiredTypeId(dto.getTypeId());
        requireType(typeId);
        assertItemValueUnique(typeId, dto.getItemValue(), null);
        Instant now = Instant.now();
        SysDictItemEntity entity = mapItem(new SysDictItemEntity(), dto, typeId);
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreateTime(now);
        entity.setLastModifyTime(now);
        dictItemMapper.insert(entity);
        return entity;
    }

    /**
     * 更新字典项。
     *
     * @param id  ID
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Transactional
    public SysDictItemEntity updateItem(Long id, SysDictItemSaveDto dto) {
        SysDictItemEntity entity = requireItem(id);
        Long typeId = entity.getTypeId();
        if (StringUtils.hasText(dto.getTypeId())) {
            typeId = parseRequiredTypeId(dto.getTypeId());
            requireType(typeId);
        }
        assertItemValueUnique(typeId, dto.getItemValue(), id);
        mapItem(entity, dto, typeId);
        entity.setLastModifyTime(Instant.now());
        dictItemMapper.updateById(entity);
        return entity;
    }

    /**
     * 删除字典项。
     *
     * @param id ID
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Transactional
    public void deleteItem(Long id) {
        requireItem(id);
        dictItemMapper.deleteById(id);
    }

    private SysDictTypeEntity requireType(Long id) {
        SysDictTypeEntity entity = dictTypeMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字典类型不存在");
        }
        return entity;
    }

    private SysDictItemEntity requireItem(Long id) {
        SysDictItemEntity entity = dictItemMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字典项不存在");
        }
        return entity;
    }

    private void assertTypeCodeUnique(String code, Long excludeId) {
        Long cnt = dictTypeMapper.selectCount(Wrappers.<SysDictTypeEntity>lambdaQuery()
                .eq(SysDictTypeEntity::getCode, code)
                .ne(excludeId != null, SysDictTypeEntity::getId, excludeId));
        if (cnt != null && cnt > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "字典编码已存在");
        }
    }

    private void assertItemValueUnique(Long typeId, String itemValue, Long excludeId) {
        Long cnt = dictItemMapper.selectCount(Wrappers.<SysDictItemEntity>lambdaQuery()
                .eq(SysDictItemEntity::getTypeId, typeId)
                .eq(SysDictItemEntity::getItemValue, itemValue)
                .ne(excludeId != null, SysDictItemEntity::getId, excludeId));
        if (cnt != null && cnt > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "字典值已存在");
        }
    }

    private SysDictTypeEntity mapType(SysDictTypeEntity entity, SysDictTypeSaveDto dto) {
        return entity
                .setCode(dto.getCode().trim())
                .setName(dto.getName().trim())
                .setDescription(dto.getDescription())
                .setStatus(normalizeStatus(dto.getStatus()))
                .setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
    }

    private SysDictItemEntity mapItem(SysDictItemEntity entity, SysDictItemSaveDto dto, Long typeId) {
        return entity
                .setTypeId(typeId)
                .setItemValue(dto.getItemValue().trim())
                .setItemLabel(dto.getItemLabel().trim())
                .setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder())
                .setStatus(normalizeStatus(dto.getStatus()))
                .setRemark(dto.getRemark());
    }

    private Long parseRequiredTypeId(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "字典类型 ID 不能为空");
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "字典类型 ID 非法");
        }
    }

    private String normalizeStatus(String raw) {
        String v = StringUtils.hasText(raw) ? raw.trim().toUpperCase(Locale.ROOT) : "ENABLED";
        if (!STATUSES.contains(v)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的状态");
        }
        return v;
    }
}
