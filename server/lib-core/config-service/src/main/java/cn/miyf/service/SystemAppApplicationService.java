package cn.miyf.service;

import cn.miyf.bean.dto.SysAppSaveDto;
import cn.miyf.bean.entity.SysAppEntity;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.repository.mapper.SysAppMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 系统应用应用服务。
 *
 * @author XieMingJie
 * @since 2026-09-07
 */
@Service
public class SystemAppApplicationService {

    private static final Set<String> STATUSES = Set.of("ENABLED", "DISABLED");

    private final SysAppMapper appMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public SystemAppApplicationService(SysAppMapper appMapper, SnowflakeIdGenerator snowflakeIdGenerator) {
        this.appMapper = appMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    /**
     * 应用列表。
     *
     * @param keyword 关键字
     * @param status  状态过滤
     * @return 列表
     * @history 1.00 2026-09-07 XieMingJie Created.
     */
    public List<SysAppEntity> list(String keyword, String status) {
        List<SysAppEntity> apps = appMapper.selectList(Wrappers.<SysAppEntity>lambdaQuery()
                .eq(StringUtils.hasText(status), SysAppEntity::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysAppEntity::getCode, keyword)
                        .or()
                        .like(SysAppEntity::getName, keyword))
                .orderByAsc(SysAppEntity::getSortOrder)
                .orderByAsc(SysAppEntity::getCode));
        Map<String, Integer> permCounts = loadCounts(appMapper.countPermissionsByProduct());
        Map<String, Integer> menuCounts = loadCounts(appMapper.countMenusByProduct());
        for (SysAppEntity app : apps) {
            String code = app.getCode() == null ? "" : app.getCode().toLowerCase(Locale.ROOT);
            app.setPermissionCount(permCounts.getOrDefault(code, 0));
            app.setMenuCount(menuCounts.getOrDefault(code, 0));
        }
        return apps;
    }

    private Map<String, Integer> loadCounts(List<Map<String, Object>> rows) {
        Map<String, Integer> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (Map<String, Object> row : rows) {
            Object product = row.get("product");
            if (product == null) {
                product = row.get("PRODUCT");
            }
            Object cnt = row.get("cnt");
            if (cnt == null) {
                cnt = row.get("CNT");
            }
            if (product == null || cnt == null) {
                continue;
            }
            map.put(String.valueOf(product).toLowerCase(Locale.ROOT), ((Number) cnt).intValue());
        }
        return map;
    }

    /**
     * 创建应用。
     *
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-07 XieMingJie Created.
     */
    @Transactional
    public SysAppEntity create(SysAppSaveDto dto) {
        String code = normalizeCode(dto.getCode());
        if (appMapper.selectByCode(code) != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "应用编码已存在");
        }
        Instant now = Instant.now();
        SysAppEntity entity = new SysAppEntity()
                .setCode(code)
                .setName(dto.getName().trim())
                .setDescription(trimToNull(dto.getDescription()))
                .setIcon(trimToNull(dto.getIcon()))
                .setHomePath(trimToNull(dto.getHomePath()))
                .setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder())
                .setStatus(normalizeStatus(dto.getStatus()))
                .setBuiltIn(false);
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreateTime(now);
        entity.setLastModifyTime(now);
        appMapper.insert(entity);
        return entity;
    }

    /**
     * 更新应用。
     *
     * @param id  ID
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-07 XieMingJie Created.
     */
    @Transactional
    public SysAppEntity update(Long id, SysAppSaveDto dto) {
        SysAppEntity entity = require(id);
        String code = normalizeCode(dto.getCode());
        SysAppEntity byCode = appMapper.selectByCode(code);
        if (byCode != null && !Objects.equals(byCode.getId(), id)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "应用编码已存在");
        }
        if (Boolean.TRUE.equals(entity.getBuiltIn()) && !code.equals(entity.getCode())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "内置应用编码不可修改");
        }
        entity.setCode(code);
        entity.setName(dto.getName().trim());
        entity.setDescription(trimToNull(dto.getDescription()));
        entity.setIcon(trimToNull(dto.getIcon()));
        entity.setHomePath(trimToNull(dto.getHomePath()));
        if (dto.getSortOrder() != null) {
            entity.setSortOrder(dto.getSortOrder());
        }
        entity.setStatus(normalizeStatus(dto.getStatus()));
        entity.setLastModifyTime(Instant.now());
        appMapper.updateById(entity);
        return entity;
    }

    /**
     * 删除应用（内置不可删）。
     *
     * @param id ID
     * @history 1.00 2026-09-07 XieMingJie Created.
     */
    @Transactional
    public void delete(Long id) {
        SysAppEntity entity = require(id);
        if (Boolean.TRUE.equals(entity.getBuiltIn())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "内置应用不可删除");
        }
        appMapper.deleteById(id);
    }

    private SysAppEntity require(Long id) {
        SysAppEntity entity = appMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "应用不存在");
        }
        return entity;
    }

    private static String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "应用编码不能为空");
        }
        return code.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeStatus(String status) {
        String s = StringUtils.hasText(status) ? status.trim().toUpperCase(Locale.ROOT) : "ENABLED";
        if (!STATUSES.contains(s)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态无效");
        }
        return s;
    }

    private static String trimToNull(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return raw.trim();
    }
}
