package cn.miyf.auth.infrastructure.huawei;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 开发环境华为登录：授权码直接映射 unionId，不访问华为。
 * <p>
 * 例：authorizationCode=mock:cook → unionId=mock_union_cook。裸码 mock 映射为 mock_union_mock。
 *
 * @author XieMingJie
 * @history 1.00 2026-09-19 XieMingJie Created.
 * @since 2026-09-19
 */
@Component
@ConditionalOnProperty(prefix = "app.auth.huawei", name = "mock-enabled", havingValue = "true")
public class MockHuaweiAccountClient implements HuaweiAccountClient {

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-19 XieMingJie Created.
     */
    @Override
    public HuaweiIdentity exchange(String authorizationCode) {
        String safe = authorizationCode == null ? "" : authorizationCode.trim();
        if (safe.startsWith("mock:")) {
            safe = safe.substring("mock:".length()).trim();
        }
        if (safe.isEmpty()) {
            safe = "dev";
        }
        return new HuaweiIdentity("mock_open_" + safe, "mock_union_" + safe, "华为用户", null);
    }
}
