package cn.miyf.notification.controller;

import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.auth.security.SystemSettingsPopedom;
import cn.miyf.common.ApiResult;
import cn.miyf.notification.InboxItem;
import cn.miyf.notification.NotificationApplicationService;
import cn.miyf.notification.dto.NotifySendDto;
import cn.miyf.notification.dto.NotifyTemplateSaveDto;
import cn.miyf.notification.vo.NotifySendLogVo;
import cn.miyf.notification.vo.NotifyTemplateVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * 覆盖发送、站内信、模板与发送流水。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Tag(name = "系统-通知")
@SystemSettingsPopedom
@RestController
@RequestMapping("/admin/system/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationApplicationService notificationApplicationService;

    
    /**
     * 发送通知（直发或模板），并写入发送流水。
     *
     * @param body 发送请求
     * @return 空成功结果
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Operation(summary = "发送通知（直发或模板；写入发送流水）")
    @MiyfPermission(code = "sys:notify:send")
    @PostMapping("/send")
    public ApiResult<Void> send(@RequestBody NotifySendDto body) {
        notificationApplicationService.send(body);
        return ApiResult.ok();
    }

    /**
     * 查询指定用户的站内信列表。
     *
     * @param userKey 用户标识
     * @param limit   可选条数上限
     * @return 站内信列表
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Operation(summary = "站内信列表")
    @MiyfPermission(code = "sys:notify:list")
    @GetMapping("/inbox")
    public ApiResult<List<InboxItem>> inbox(@RequestParam String userKey,
                                            @RequestParam(required = false) Integer limit) {
        return ApiResult.ok(notificationApplicationService.listInbox(userKey, limit));
    }

    /**
     * 标记站内信已读。
     *
     * @param id      站内信 ID
     * @param userKey 可选用户标识（校验归属）
     * @return 更新后的站内信
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Operation(summary = "标记站内信已读")
    @MiyfPermission(code = "sys:notify:read")
    @PostMapping("/inbox/{id}/read")
    public ApiResult<InboxItem> markRead(@PathVariable Long id,
                                         @RequestParam(required = false) String userKey) {
        return ApiResult.ok(notificationApplicationService.markRead(id, userKey));
    }

    /**
     * 通知模板列表，可按渠道与状态过滤。
     *
     * @param channel 可选渠道
     * @param status  可选状态
     * @return 模板 VO 列表
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Operation(summary = "通知模板列表")
    @MiyfPermission(code = "sys:notify:list")
    @GetMapping("/templates")
    public ApiResult<List<NotifyTemplateVo>> listTemplates(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status) {
        return ApiResult.ok(notificationApplicationService.listTemplates(channel, status));
    }

    /**
     * 创建通知模板。
     *
     * @param dto 模板保存请求
     * @return 新建模板 VO
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Operation(summary = "创建通知模板")
    @MiyfPermission(code = "sys:notify:send")
    @PostMapping("/templates")
    public ApiResult<NotifyTemplateVo> createTemplate(@Valid @RequestBody NotifyTemplateSaveDto dto) {
        return ApiResult.ok(notificationApplicationService.createTemplate(dto));
    }

    /**
     * 更新通知模板。
     *
     * @param id  模板 ID
     * @param dto 模板保存请求
     * @return 更新后模板 VO
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Operation(summary = "更新通知模板")
    @MiyfPermission(code = "sys:notify:send")
    @PutMapping("/templates/{id}")
    public ApiResult<NotifyTemplateVo> updateTemplate(@PathVariable Long id,
                                                      @Valid @RequestBody NotifyTemplateSaveDto dto) {
        return ApiResult.ok(notificationApplicationService.updateTemplate(id, dto));
    }

    /**
     * 删除通知模板。
     *
     * @param id 模板 ID
     * @return 空成功结果
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    @Operation(summary = "删除通知模板")
    @MiyfPermission(code = "sys:notify:send")
    @DeleteMapping("/templates/{id}")
    public ApiResult<Void> deleteTemplate(@PathVariable Long id) {
        notificationApplicationService.deleteTemplate(id);
        return ApiResult.ok();
    }

    /**
     * 发送历史流水。
     *
     * @param channel 可选渠道
     * @param status  可选状态
     * @param limit   可选条数上限
     * @return 发送日志 VO 列表
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
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
