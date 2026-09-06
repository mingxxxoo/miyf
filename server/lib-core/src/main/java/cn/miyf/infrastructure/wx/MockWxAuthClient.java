package cn.miyf.infrastructure.wx;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Mock 微信登录：开发环境用 code 直接映射 openid。
 * <p>
 * 例：code=dev-user-1 → openid=mock_dev-user-1
 *
 * @author XieMingJie
 * @since 2026-09-04 17:06
 */
@Component
@ConditionalOnProperty(prefix = "wx.auth", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockWxAuthClient implements WxAuthClient {

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-04 17:06 XieMingJie Created.
     */
    @Override
    public WxSession code2Session(String code) {
        String safe = (code == null || code.isBlank()) ? "anonymous" : code.trim();
        return new WxSession("mock_" + safe, null);
    }
}
