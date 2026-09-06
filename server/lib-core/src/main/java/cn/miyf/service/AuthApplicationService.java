package cn.miyf.service;

import cn.miyf.bean.dto.AdminLoginDto;
import cn.miyf.bean.entity.SysPermissionEntity;
import cn.miyf.bean.entity.SysRoleEntity;
import cn.miyf.bean.entity.SysUserEntity;
import cn.miyf.bean.vo.LoginVo;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.infrastructure.redis.RedisRateLimiter;
import cn.miyf.repository.mapper.SysPermissionMapper;
import cn.miyf.repository.mapper.SysRoleMapper;
import cn.miyf.repository.mapper.SysUserMapper;
import cn.miyf.security.AuthPrincipal;
import cn.miyf.security.JwtService;
import cn.miyf.security.PrincipalType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 平台认证应用服务：管理员（sys_user）登录。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@Service
public class AuthApplicationService extends BaseApplicationService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisRateLimiter redisRateLimiter;

    /**
     * 构造认证服务。
     *
     * @param sysUserMapper        系统用户 Mapper
     * @param sysRoleMapper        角色 Mapper
     * @param sysPermissionMapper  权限 Mapper
     * @param passwordEncoder      密码编码器
     * @param jwtService           JWT 服务
     * @param redisRateLimiter     登录限流
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public AuthApplicationService(SysUserMapper sysUserMapper,
                                  SysRoleMapper sysRoleMapper,
                                  SysPermissionMapper sysPermissionMapper,
                                  PasswordEncoder passwordEncoder,
                                  JwtService jwtService,
                                  RedisRateLimiter redisRateLimiter) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.redisRateLimiter = redisRateLimiter;
    }

    /**
     * 管理员登录（sys_user）。
     *
     * @param dto 登录请求
     * @return 登录结果
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    public LoginVo adminLogin(AdminLoginDto dto) {
        String principalKey = "admin:" + dto.getUsername().trim().toLowerCase();
        redisRateLimiter.assertLoginAllowed(principalKey);
        try {
            SysUserEntity user = sysUserMapper.selectByUsername(dto.getUsername());
            if (user == null) {
                throw new BusinessException(ErrorCode.LOGIN_FAILED);
            }
            if (!"ENABLED".equals(user.getStatus())) {
                throw new BusinessException(ErrorCode.USER_DISABLED);
            }
            if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
                throw new BusinessException(ErrorCode.LOGIN_FAILED);
            }
            List<String> roleCodes = sysRoleMapper.findByUserId(user.getId()).stream()
                    .map(SysRoleEntity::getCode)
                    .toList();
            List<String> permissionCodes = sysPermissionMapper.findByUserId(user.getId()).stream()
                    .map(SysPermissionEntity::getCode)
                    .toList();
            AuthPrincipal principal = new AuthPrincipal(
                    user.getId(),
                    user.getUsername(),
                    PrincipalType.ADMIN,
                    permissionCodes,
                    true
            );
            redisRateLimiter.clearLoginFailures(principalKey);
            return toLoginVo(principal, user.getNickname(), roleCodes);
        } catch (BusinessException ex) {
            if (ex.getCode() == ErrorCode.LOGIN_FAILED.getCode()) {
                redisRateLimiter.recordLoginFailure(principalKey);
            }
            throw ex;
        }
    }

    /**
     * 组装登录响应。
     *
     * @param principal   主体
     * @param displayName 展示名
     * @param roles       角色码
     * @return LoginVo
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    private LoginVo toLoginVo(AuthPrincipal principal, String displayName, List<String> roles) {
        return new LoginVo()
                .setToken(jwtService.createToken(principal))
                .setExpireSeconds(jwtService.getExpireSeconds())
                .setUserId(principal.getId())
                .setDisplayName(displayName)
                .setPrincipalType(principal.getType().name())
                .setPermissions(principal.getPermissions())
                .setRoles(roles);
    }
}
