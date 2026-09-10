package cn.miyf.health.security;

import cn.miyf.auth.security.PopedomScope;

/**
 * 健康业务权限组编码常量。
 *
 * @author XieMingJie
 * @since 2026-09-07
 * @history 1.00 2026-09-07 XieMingJie Created.
 */
public final class HealthPopedomCodes {

    private HealthPopedomCodes() {
    }

    /** 健康个人端。 */
    public static final class Personal {
        public static final String CODE = "12010000";
        public static final String SERVICE = "健康服务";
        public static final String PRODUCT = "health";
        public static final PopedomScope SCOPE = PopedomScope.PERSONAL;
        public static final int SORT = 20;

        private Personal() {
        }
    }

    /** 健康单位端（预留）。 */
    public static final class Org {
        public static final String CODE = "12020000";
        public static final String SERVICE = "健康服务";
        public static final String PRODUCT = "health";
        public static final PopedomScope SCOPE = PopedomScope.ORG;
        public static final int SORT = 20;

        private Org() {
        }
    }

    /** 健康超管端。 */
    public static final class Admin {
        public static final String CODE = "12030000";
        public static final String SERVICE = "健康服务";
        public static final String PRODUCT = "health";
        public static final PopedomScope SCOPE = PopedomScope.SUPER;
        public static final int SORT = 20;

        private Admin() {
        }
    }
}
