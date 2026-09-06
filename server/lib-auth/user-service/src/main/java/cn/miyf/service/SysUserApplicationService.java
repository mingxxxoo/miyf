package cn.miyf.service;

import cn.miyf.bean.dto.SysUserSaveDto;
import cn.miyf.bean.entity.SysUserEntity;
import cn.miyf.bean.entity.SysUserRoleEntity;
import cn.miyf.bean.vo.SysUserVo;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.id.SnowflakeIdGenerator;
import cn.miyf.repository.mapper.SysUserMapper;
import cn.miyf.repository.mapper.SysUserRoleMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 系统用户（管理员账号）应用服务。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Service
public class SysUserApplicationService extends BaseApplicationService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final PasswordEncoder passwordEncoder;

    public SysUserApplicationService(SysUserMapper userMapper,
                                     SysUserRoleMapper userRoleMapper,
                                     SnowflakeIdGenerator snowflakeIdGenerator,
                                     PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户列表（脱敏）。
     *
     * @return 列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     * @history 1.01 2026-09-06 XieMingJie 从 IamApplicationService 拆出。
     */
    public List<SysUserVo> listUsers() {
        return userMapper.selectList(Wrappers.<SysUserEntity>lambdaQuery()
                        .orderByDesc(SysUserEntity::getCreatedAt))
                .stream()
                .map(this::toUserVo)
                .toList();
    }

    /**
     * 创建用户。
     *
     * @param dto 请求
     * @return VO
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public SysUserVo createUser(SysUserSaveDto dto) {
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码不能为空");
        }
        Instant now = Instant.now();
        SysUserEntity entity = new SysUserEntity()
                .setOrgUnitId(parseId(dto.getOrgUnitId()))
                .setUsername(dto.getUsername())
                .setPasswordHash(passwordEncoder.encode(dto.getPassword()))
                .setNickname(dto.getNickname())
                .setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "ENABLED");
        entity.setId(snowflakeIdGenerator.nextId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        userMapper.insert(entity);
        bindUserRoles(entity.getId(), parseIds(dto.getRoleIds()));
        return toUserVo(entity);
    }

    /**
     * 更新用户。
     *
     * @param id  ID
     * @param dto 请求
     * @return VO
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public SysUserVo updateUser(Long id, SysUserSaveDto dto) {
        SysUserEntity entity = requireUser(id);
        entity.setOrgUnitId(parseId(dto.getOrgUnitId()));
        entity.setUsername(dto.getUsername());
        entity.setNickname(dto.getNickname());
        if (StringUtils.hasText(dto.getStatus())) {
            entity.setStatus(dto.getStatus());
        }
        if (StringUtils.hasText(dto.getPassword())) {
            entity.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        entity.setUpdatedAt(Instant.now());
        userMapper.updateById(entity);
        if (dto.getRoleIds() != null) {
            bindUserRoles(id, parseIds(dto.getRoleIds()));
        }
        return toUserVo(entity);
    }

    /**
     * 删除用户。
     *
     * @param id ID
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Transactional
    public void deleteUser(Long id) {
        requireUser(id);
        userRoleMapper.deleteByUserId(id);
        userMapper.deleteById(id);
    }

    private SysUserVo toUserVo(SysUserEntity entity) {
        return new SysUserVo()
                .setId(entity.getId())
                .setOrgUnitId(entity.getOrgUnitId())
                .setUsername(entity.getUsername())
                .setNickname(entity.getNickname())
                .setStatus(entity.getStatus())
                .setCreatedAt(entity.getCreatedAt())
                .setUpdatedAt(entity.getUpdatedAt());
    }

    private SysUserEntity requireUser(Long id) {
        SysUserEntity entity = userMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        return entity;
    }

    private void bindUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        if (roleIds == null) {
            return;
        }
        Instant now = Instant.now();
        for (Long roleId : roleIds) {
            SysUserRoleEntity bind = new SysUserRoleEntity()
                    .setId(snowflakeIdGenerator.nextId())
                    .setUserId(userId)
                    .setRoleId(roleId)
                    .setCreatedAt(now);
            userRoleMapper.insert(bind);
        }
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

    private List<Long> parseIds(List<String> raws) {
        if (raws == null) {
            return null;
        }
        return raws.stream().map(this::parseId).filter(Objects::nonNull).toList();
    }
}
