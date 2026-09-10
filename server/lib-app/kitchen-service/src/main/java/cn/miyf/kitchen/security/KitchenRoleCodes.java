package cn.miyf.kitchen.security;

/**
 * 厨房模块角色编码。
 *
 * @author XieMingJie
 * @since 2026-09-10
 * @history 1.00 2026-09-10 XieMingJie Created.
 */
public final class KitchenRoleCodes {

    private KitchenRoleCodes() {
    }

    /** 厨房默认角色（所有厨房用户可拥有）。 */
    public static final String DEFAULT = "KITCHEN_DEFAULT";

    /** 厨房单位角色。 */
    public static final String ORG = "KITCHEN_ORG";
}
