package cn.miyf.auth.security;

/**
 * 权限域（个人 / 单位 / 超管）。
 * <p>
 * 鉴权与权限树层级必须使用本枚举判断，禁止用展示名字符串比对。
 *
 * @author XieMingJie
 * @since 2026-09-10
 * @history 1.00 2026-09-10 XieMingJie Created.
 */
public enum PopedomScope {

    /** 个人端（C 端 / 小程序）。 */
    PERSONAL("个人"),

    /** 单位端。 */
    ORG("单位"),

    /** 超管端（后台 / 平台）。 */
    SUPER("超管");

    private final String label;

    PopedomScope(String label) {
        this.label = label;
    }

    /**
     * 展示用中文标签（仅用于树名称拼装，不用于鉴权判断）。
     *
     * @return 标签
     */
    public String getLabel() {
        return label;
    }
}
