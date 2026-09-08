package cn.miyf.organization.controller;

import cn.miyf.auth.security.IamAdminPopedom;
import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.organization.bean.dto.OrgUnitSaveDto;
import cn.miyf.organization.bean.entity.SysOrgUnitEntity;
import cn.miyf.organization.bean.vo.SysOrgUnitTreeVo;
import cn.miyf.organization.service.OrganizationApplicationService;
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
 * 组织单位管理接口。
 * 列表与写操作受当前登录用户数据范围（DataScope）约束；列表返回无限极树。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Tag(name = "IAM-组织单位")
@IamAdminPopedom
@RestController
@RequestMapping("/api/iam/org-units")
public class IamOrgUnitController {

    private final OrganizationApplicationService organizationApplicationService;

    /**
     * 构造控制器。
     *
     * @param organizationApplicationService 组织应用服务
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public IamOrgUnitController(OrganizationApplicationService organizationApplicationService) {
        this.organizationApplicationService = organizationApplicationService;
    }

    /**
     * 查询当前用户数据范围内可见的组织单位无限极树。
     *
     * @return 组织树根列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "组织单位树")
    @MiyfPermission(code = "iam:org:list")
    @GetMapping
    public ApiResult<List<SysOrgUnitTreeVo>> list() {
        return ApiResult.ok(organizationApplicationService.listOrgUnitsTree());
    }

    /**
     * 创建组织单位。
     *
     * @param dto 保存请求
     * @return 新建组织单位
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "创建组织单位")
    @MiyfPermission(code = "iam:org:create")
    @PostMapping
    public ApiResult<SysOrgUnitEntity> create(@Valid @RequestBody OrgUnitSaveDto dto) {
        return ApiResult.ok(organizationApplicationService.createOrgUnit(dto));
    }

    /**
     * 更新组织单位。
     *
     * @param id  组织单位 ID
     * @param dto 保存请求
     * @return 更新后组织单位
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "更新组织单位")
    @MiyfPermission(code = "iam:org:update")
    @PutMapping("/{id}")
    public ApiResult<SysOrgUnitEntity> update(@PathVariable Long id, @Valid @RequestBody OrgUnitSaveDto dto) {
        return ApiResult.ok(organizationApplicationService.updateOrgUnit(id, dto));
    }

    /**
     * 删除组织单位。
     *
     * @param id 组织单位 ID
     * @return 空成功结果
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "删除组织单位")
    @MiyfPermission(code = "iam:org:delete")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        organizationApplicationService.deleteOrgUnit(id);
        return ApiResult.ok();
    }
}
