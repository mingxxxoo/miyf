package cn.miyf.notification.controller;

import cn.miyf.common.ApiResult;
import cn.miyf.notification.InboxItem;
import cn.miyf.notification.NotificationApplicationService;
import cn.miyf.notification.dto.NotifySendDto;
import cn.miyf.notification.dto.NotifyTemplateSaveDto;
import cn.miyf.notification.vo.NotifySendLogVo;
import cn.miyf.notification.vo.NotifyTemplateVo;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.SystemSettingsPopedom;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 通知管理 API。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Tag(name = "系统-通知")
@SystemSettingsPopedom
@RestController
@RequestMapping("/api/admin/system/notifications")
public class AdminNotificationController {

    private final NotificationApplicationService notificationApplicationService;

    public AdminNotificationController(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    @Operation(summary = "发送通知（直发或模板；写入发送流水）")
    @MiyfPermission(code = "sys:notify:send")
    @PostMapping("/send")
    public ApiResult<Void> send(@RequestBody NotifySendDto body) {
        notificationApplicationService.send(body);
        return ApiResult.ok();
    }

    @Operation(summary = "站内信列表")
    @MiyfPermission(code = "sys:notify:list")
    @GetMapping("/inbox")
    public ApiResult<List<InboxItem>> inbox(@RequestParam String userKey,
                                            @RequestParam(required = false) Integer limit) {
        return ApiResult.ok(notificationApplicationService.listInbox(userKey, limit));
    }

    @Operation(summary = "标记站内信已读")
    @MiyfPermission(code = "sys:notify:read")
    @PostMapping("/inbox/{id}/read")
    public ApiResult<InboxItem> markRead(@PathVariable Long id,
                                         @RequestParam(required = false) String userKey) {
        return ApiResult.ok(notificationApplicationService.markRead(id, userKey));
    }

    @Operation(summary = "通知模板列表")
    @MiyfPermission(code = "sys:notify:list")
    @GetMapping("/templates")
    public ApiResult<List<NotifyTemplateVo>> listTemplates(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status) {
        return ApiResult.ok(notificationApplicationService.listTemplates(channel, status));
    }

    @Operation(summary = "创建通知模板")
    @MiyfPermission(code = "sys:notify:send")
    @PostMapping("/templates")
    public ApiResult<NotifyTemplateVo> createTemplate(@Valid @RequestBody NotifyTemplateSaveDto dto) {
        return ApiResult.ok(notificationApplicationService.createTemplate(dto));
    }

    @Operation(summary = "更新通知模板")
    @MiyfPermission(code = "sys:notify:send")
    @PutMapping("/templates/{id}")
    public ApiResult<NotifyTemplateVo> updateTemplate(@PathVariable Long id,
                                                      @Valid @RequestBody NotifyTemplateSaveDto dto) {
        return ApiResult.ok(notificationApplicationService.updateTemplate(id, dto));
    }

    @Operation(summary = "删除通知模板")
    @MiyfPermission(code = "sys:notify:send")
    @DeleteMapping("/templates/{id}")
    public ApiResult<Void> deleteTemplate(@PathVariable Long id) {
        notificationApplicationService.deleteTemplate(id);
        return ApiResult.ok();
    }

    @Operation(summary = "发送历史")
    @MiyfPermission(code = "sys:notify:list")
    @GetMapping("/send-logs")
    public ApiResult<List<NotifySendLogVo>> listSendLogs(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return ApiResult.ok(notificationApplicationService.listSendLogs(channel, status, limit));
    }
}
