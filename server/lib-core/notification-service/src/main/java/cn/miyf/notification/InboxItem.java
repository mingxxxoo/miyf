package cn.miyf.notification;

import java.time.Instant;

/**
 * 站内信列表项。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
public record InboxItem(
        String id,
        String userKey,
        String title,
        String content,
        Instant createdAt,
        boolean read
) {
}
