package cn.miyf.notification.controller;

import cn.miyf.common.ApiResult;
import cn.miyf.notification.InboxItem;
import cn.miyf.notification.NotificationApplicationService;
import cn.miyf.notification.NotificationChannel;
import cn.miyf.notification.NotificationMessage;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.PopedomGroup;
import cn.miyf.security.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 通知管理 API。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Tag(name = "系统-通知")
@PopedomGroup(value = "10040000", name = "系统设置", product = "system", sort = 8)
@RestController
@RequestMapping("/api/admin/system/notifications")
public class AdminNotificationController {

    private final NotificationApplicationService notificationApplicationService;

    public AdminNotificationController(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    @Operation(summary = "发送通知（邮件/短信按适配器，站内信落库）")
    @MiyfPermission(code = "sys:notify:send", name = "发送通知", groupCode = "sys_notify", groupName = "通知")
    @RequirePermission({"sys:notify:send"})
    @PostMapping("/send")
    public ApiResult<Void> send(@RequestBody Map<String, String> body) {
        NotificationChannel channel = NotificationChannel.valueOf(
                body.getOrDefault("channel", "INBOX").trim().toUpperCase());
        notificationApplicationService.send(new NotificationMessage()
                .setChannel(channel)
                .setTo(body.get("to"))
                .setTitle(body.getOrDefault("title", ""))
                .setContent(body.getOrDefault("content", "")));
        return ApiResult.ok();
    }

    @Operation(summary = "站内信列表")
    @MiyfPermission(code = "sys:notify:list", name = "通知列表", groupCode = "sys_notify", groupName = "通知")
    @RequirePermission({"sys:notify:list"})
    @GetMapping("/inbox")
    public ApiResult<List<InboxItem>> inbox(@RequestParam String userKey,
                                            @RequestParam(required = false) Integer limit) {
        return ApiResult.ok(notificationApplicationService.listInbox(userKey, limit));
    }

    @Operation(summary = "标记站内信已读")
    @MiyfPermission(code = "sys:notify:read", name = "标记已读", groupCode = "sys_notify", groupName = "通知")
    @RequirePermission({"sys:notify:read"})
    @PostMapping("/inbox/{id}/read")
    public ApiResult<InboxItem> markRead(@PathVariable Long id,
                                         @RequestParam(required = false) String userKey) {
        return ApiResult.ok(notificationApplicationService.markRead(id, userKey));
    }
}
