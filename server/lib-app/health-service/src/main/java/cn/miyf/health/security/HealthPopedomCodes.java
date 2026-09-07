package cn.miyf.health.security;

/**
 * 健康业务权限组编码常量。
 *
 * @author XieMingJie
 * @since 2026-09-07
 */
public final class HealthPopedomCodes {

    private HealthPopedomCodes() {
    }

    /** 健康个人端。 */
    public static final class Personal {
        public static final String CODE = "12010000";
        public static final String NAME = "个人";
        public static final String PRODUCT = "health";
        public static final int SORT = 20;

        private Personal() {
        }
    }
}
