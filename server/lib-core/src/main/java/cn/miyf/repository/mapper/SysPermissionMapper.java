package cn.miyf.repository.mapper;

import cn.miyf.bean.entity.SysPermissionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限 Mapper；按用户解析权限码见 XML。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermissionEntity> {

    /**
     * 按权限码查询。
     *
     * @param code 权限码
     * @return 权限
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    SysPermissionEntity selectByCode(@Param("code") String code);

    /**
     * 按用户解析权限（user→role→perm_group→permission）。
     *
     * @param userId 用户 ID
     * @return 权限列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<SysPermissionEntity> findByUserId(@Param("userId") Long userId);

    /**
     * 查询角色已绑定权限组下的全部权限。
     *
     * @param roleId 角色 ID
     * @return 权限列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<SysPermissionEntity> findByRoleId(@Param("roleId") Long roleId);
}
