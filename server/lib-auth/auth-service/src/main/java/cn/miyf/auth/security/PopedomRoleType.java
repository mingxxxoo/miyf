package cn.miyf.auth.security;

/**
 * 扫描声明的角色类型。
 * <p>
 * {@link #DEFAULT}：模块默认角色（业务上所有人可拥有）；
 * {@link #ORG}：单位角色；
 * {@link #SUPER}：超管角色（仅公共模块定义，拥有全部权限组）。
 *
 * @author XieMingJie
 * @since 2026-09-10
 * @history 1.00 2026-09-10 XieMingJie Created.
 */
public enum PopedomRoleType {

    /** 模块默认角色。 */
    DEFAULT,

    /** 单位角色。 */
    ORG,

    /** 超管角色（全局）。 */
    SUPER
}
