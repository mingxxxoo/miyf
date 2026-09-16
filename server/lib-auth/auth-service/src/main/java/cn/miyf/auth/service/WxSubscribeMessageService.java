package cn.miyf.auth.service;

import cn.miyf.auth.infrastructure.wx.WxSubscribeMessageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 微信订阅消息业务门面，供厨房等模块调用。
 * 开关与模板等业务参数由调用方从系统配置读取后传入。
 *
 * @author XieMingJie
 * @since 2026-09-16
 */
@Service
@RequiredArgsConstructor
public class WxSubscribeMessageService {

    private final WxSubscribeMessageClient client;

    /**
     * 发送订阅消息。
     *
     * @param openid           接收 openid
     * @param templateId       模板 ID
     * @param page             跳转页
     * @param miniprogramState 小程序版本态 formal/trial/developer
     * @param data             模板字段
     * @return 是否成功
     */
    public boolean send(String openid, String templateId, String page, String miniprogramState,
                        Map<String, String> data) {
        return client.send(openid, templateId, page, miniprogramState, data);
    }
}
