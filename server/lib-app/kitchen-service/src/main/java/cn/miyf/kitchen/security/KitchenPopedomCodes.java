package cn.miyf.kitchen.security;

import cn.miyf.auth.security.PopedomScope;

/**
 * 厨房业务权限组编码常量。
 *
 * @author XieMingJie
 * @since 2026-09-07
 * @history 1.00 2026-09-07 XieMingJie Created.
 */
public final class KitchenPopedomCodes {

    private KitchenPopedomCodes() {
    }

    /** 厨房个人端（普通用户 / 小程序）。 */
    public static final class Personal {
        public static final String CODE = "11010000";
        public static final String SERVICE = "厨房服务";
        public static final String PRODUCT = "kitchen";
        public static final PopedomScope SCOPE = PopedomScope.PERSONAL;
        public static final int SORT = 10;

        private Personal() {
        }
    }

    /** 厨房单位端。 */
    public static final class Org {
        public static final String CODE = "11020000";
        public static final String SERVICE = "厨房服务";
        public static final String PRODUCT = "kitchen";
        public static final PopedomScope SCOPE = PopedomScope.ORG;
        public static final int SORT = 10;

        private Org() {
        }
    }

    /** 厨房超管端（管理后台）。 */
    public static final class Admin {
        public static final String CODE = "11030000";
        public static final String SERVICE = "厨房服务";
        public static final String PRODUCT = "kitchen";
        public static final PopedomScope SCOPE = PopedomScope.SUPER;
        public static final int SORT = 10;

        private Admin() {
        }
    }
}
