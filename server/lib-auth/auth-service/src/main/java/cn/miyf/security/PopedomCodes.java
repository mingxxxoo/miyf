package cn.miyf.security;

/**
 * 平台侧权限组编码常量（IAM / 系统设置）。
 * <p>
 * 业务模块请使用各自模块内的常量类，避免在 Controller 上手写 8 位编码。
 *
 * @author XieMingJie
 * @since 2026-09-07
 */
public final class PopedomCodes {

    private PopedomCodes() {
    }

    /** IAM 管理员域：菜单 / 角色 / 权限 / 人员 / 单位等。 */
    public static final class IamAdmin {
        public static final String CODE = "10030000";
        public static final String NAME = "管理员";
        public static final String PRODUCT = "iam";
        public static final int SORT = 5;

        private IamAdmin() {
        }
    }

    /** 系统设置域：字典 / 配置 / 应用 / 任务 / 监控 / 通知等。 */
    public static final class SystemSettings {
        public static final String CODE = "10040000";
        public static final String NAME = "系统设置";
        public static final String PRODUCT = "system";
        public static final int SORT = 8;

        private SystemSettings() {
        }
    }
}
