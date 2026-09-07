package cn.miyf.kitchen.security;

/**
 * 厨房业务权限组编码常量。
 *
 * @author XieMingJie
 * @since 2026-09-07
 */
public final class KitchenPopedomCodes {

    private KitchenPopedomCodes() {
    }

    /** 厨房个人端（预留）。 */
    public static final class Personal {
        public static final String CODE = "11010000";
        public static final String NAME = "个人";
        public static final String PRODUCT = "kitchen";
        public static final int SORT = 10;

        private Personal() {
        }
    }

    /** 厨房单位端（预留）。 */
    public static final class Org {
        public static final String CODE = "11020000";
        public static final String NAME = "单位";
        public static final String PRODUCT = "kitchen";
        public static final int SORT = 10;

        private Org() {
        }
    }

    /** 厨房管理端。 */
    public static final class Admin {
        public static final String CODE = "11030000";
        public static final String NAME = "管理员";
        public static final String PRODUCT = "kitchen";
        public static final int SORT = 10;

        private Admin() {
        }
    }
}
