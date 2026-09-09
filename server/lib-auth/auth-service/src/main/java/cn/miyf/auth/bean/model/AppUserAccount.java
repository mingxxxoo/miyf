package cn.miyf.auth.bean.model;

/**
 * 用户端账号快照（微信登录读写用，与具体业务表解耦）。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
public class AppUserAccount {

    private Long id;
    private String openid;
    private String unionid;
    private String username;
    private String nickname;
    private String phone;
    private String wechatId;
    private String avatarUrl;
    private String status;

    public Long getId() {
        return id;
    }

    public AppUserAccount setId(Long id) {
        this.id = id;
        return this;
    }

    public String getOpenid() {
        return openid;
    }

    public AppUserAccount setOpenid(String openid) {
        this.openid = openid;
        return this;
    }

    public String getUnionid() {
        return unionid;
    }

    public AppUserAccount setUnionid(String unionid) {
        this.unionid = unionid;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public AppUserAccount setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getNickname() {
        return nickname;
    }

    public AppUserAccount setNickname(String nickname) {
        this.nickname = nickname;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public AppUserAccount setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public String getWechatId() {
        return wechatId;
    }

    public AppUserAccount setWechatId(String wechatId) {
        this.wechatId = wechatId;
        return this;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public AppUserAccount setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public AppUserAccount setStatus(String status) {
        this.status = status;
        return this;
    }
}
