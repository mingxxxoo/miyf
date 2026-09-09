/**
 * 认证服务：登录、JWT、账号实体、权限注解（{@code @MiyfPermission} / {@code @RequirePermission}）。
 * <p>
 * 管理员角色/权限码由 {@link cn.miyf.auth.security.AdminAuthAuthorityLoader} 加载；
 * 用户端默认角色（{@code default_person}）由 {@link cn.miyf.auth.security.AppUserAuthAuthorityLoader} 加载（均由 permission-service 实现）。
 */
package cn.miyf.auth;
