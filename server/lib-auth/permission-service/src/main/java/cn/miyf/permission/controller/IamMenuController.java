package cn.miyf.permission.controller;

import cn.miyf.auth.security.IamAdminPopedom;
import cn.miyf.auth.security.MiyfPermission;
import cn.miyf.common.ApiResult;
import cn.miyf.permission.bean.dto.SysMenuSaveDto;
import cn.miyf.permission.bean.entity.SysMenuEntity;
import cn.miyf.permission.bean.vo.SysMenuTreeVo;
import cn.miyf.permission.service.PermissionApplicationService;
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
 * 系统菜单管理接口。
 * 维护侧栏/路由所用的菜单树节点及绑定权限码。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Tag(name = "IAM-菜单")
@IamAdminPopedom
@RestController
@RequestMapping("/api/iam/menus")
public class IamMenuController {

    private final PermissionApplicationService permissionApplicationService;

    /**
     * 构造控制器。
     *
     * @param permissionApplicationService 权限应用服务
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public IamMenuController(PermissionApplicationService permissionApplicationService) {
        this.permissionApplicationService = permissionApplicationService;
    }

    /**
     * 查询菜单无限极树（服务端 TreeUtils 组树）。
     *
     * @return 菜单树根列表
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "菜单树")
    @MiyfPermission(code = "iam:menu:list")
    @GetMapping
    public ApiResult<List<SysMenuTreeVo>> list() {
        return ApiResult.ok(permissionApplicationService.listMenusTree());
    }

    /**
     * 创建菜单节点。
     *
     * @param dto 菜单保存请求
     * @return 新建菜单
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "创建菜单")
    @MiyfPermission(code = "iam:menu:create")
    @PostMapping
    public ApiResult<SysMenuEntity> create(@Valid @RequestBody SysMenuSaveDto dto) {
        return ApiResult.ok(permissionApplicationService.createMenu(dto));
    }

    /**
     * 更新菜单节点；禁止将父级指向自身。
     *
     * @param id  菜单 ID
     * @param dto 菜单保存请求
     * @return 更新后菜单
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "更新菜单")
    @MiyfPermission(code = "iam:menu:update")
    @PutMapping("/{id}")
    public ApiResult<SysMenuEntity> update(@PathVariable Long id, @Valid @RequestBody SysMenuSaveDto dto) {
        return ApiResult.ok(permissionApplicationService.updateMenu(id, dto));
    }

    /**
     * 删除菜单节点。
     *
     * @param id 菜单 ID
     * @return 空成功结果
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    @Operation(summary = "删除菜单")
    @MiyfPermission(code = "iam:menu:delete")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        permissionApplicationService.deleteMenu(id);
        return ApiResult.ok();
    }
}
