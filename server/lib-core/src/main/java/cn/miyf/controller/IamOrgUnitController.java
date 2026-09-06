package cn.miyf.controller;

import cn.miyf.common.ApiResult;
import cn.miyf.bean.dto.OrgUnitSaveDto;
import cn.miyf.bean.entity.SysOrgUnitEntity;
import cn.miyf.service.IamApplicationService;
import cn.miyf.security.MiyfPermission;
import cn.miyf.security.RequirePermission;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 组织单位管理。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Tag(name = "IAM-组织单位")
@RestController
@RequestMapping("/api/iam/org-units")
public class IamOrgUnitController {

    private final IamApplicationService iamApplicationService;

    /**
     * 构造控制器。
     *
     * @param iamApplicationService IAM 服务
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    public IamOrgUnitController(IamApplicationService iamApplicationService) {
        this.iamApplicationService = iamApplicationService;
    }

    /**
     * 组织单位列表。
     *
     * @return 列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Operation(summary = "组织单位列表")
    @MiyfPermission(code = "iam:org:list", name = "单位列表", groupCode = "iam_org", groupName = "单位管理")
    @RequirePermission({"iam:org:list"})
    @GetMapping
    public ApiResult<List<SysOrgUnitEntity>> list() {
        return ApiResult.ok(iamApplicationService.listOrgUnits());
    }

    /**
     * 创建组织单位。
     *
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Operation(summary = "创建组织单位")
    @MiyfPermission(code = "iam:org:create", name = "创建单位", groupCode = "iam_org", groupName = "单位管理")
    @RequirePermission({"iam:org:create"})
    @PostMapping
    public ApiResult<SysOrgUnitEntity> create(@Valid @RequestBody OrgUnitSaveDto dto) {
        return ApiResult.ok(iamApplicationService.createOrgUnit(dto));
    }

    /**
     * 更新组织单位。
     *
     * @param id  ID
     * @param dto 请求
     * @return 实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Operation(summary = "更新组织单位")
    @MiyfPermission(code = "iam:org:update", name = "更新单位", groupCode = "iam_org", groupName = "单位管理")
    @RequirePermission({"iam:org:update"})
    @PutMapping("/{id}")
    public ApiResult<SysOrgUnitEntity> update(@PathVariable Long id, @Valid @RequestBody OrgUnitSaveDto dto) {
        return ApiResult.ok(iamApplicationService.updateOrgUnit(id, dto));
    }

    /**
     * 删除组织单位。
     *
     * @param id ID
     * @return 空
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    @Operation(summary = "删除组织单位")
    @MiyfPermission(code = "iam:org:delete", name = "删除单位", groupCode = "iam_org", groupName = "单位管理")
    @RequirePermission({"iam:org:delete"})
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        iamApplicationService.deleteOrgUnit(id);
        return ApiResult.ok();
    }
}
