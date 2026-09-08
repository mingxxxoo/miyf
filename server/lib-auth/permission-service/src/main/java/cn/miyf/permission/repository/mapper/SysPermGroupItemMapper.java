package cn.miyf.permission.repository.mapper;

import cn.miyf.permission.bean.entity.SysPermGroupItemEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限组条目 Mapper。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Mapper
public interface SysPermGroupItemMapper extends BaseMapper<SysPermGroupItemEntity> {

    /**
     * 按组与权限查询条目。
     *
     * @param groupId      权限组 ID
     * @param permissionId 权限 ID
     * @return 条目
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    SysPermGroupItemEntity selectByGroupAndPermission(@Param("groupId") Long groupId,
                                                      @Param("permissionId") Long permissionId);

    /**
     * 按权限删除组内条目。
     *
     * @param permissionId 权限 ID
     * @return 影响行数
     * @history 1.00 2026-09-07 XieMingJie Created.
     */
    int deleteByPermissionId(@Param("permissionId") Long permissionId);

    /**
     * 清空权限组内条目。
     *
     * @param groupId 权限组 ID
     * @return 影响行数
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    int deleteByGroupId(@Param("groupId") Long groupId);

    /**
     * 查询权限组绑定的权限 ID。
     *
     * @param groupId 权限组 ID
     * @return 权限 ID 列表
     * @history 1.00 2026-09-07 XieMingJie Created.
     */
    List<Long> selectPermissionIdsByGroupId(@Param("groupId") Long groupId);

    /**
     * 批量查询多个权限组的条目。
     *
     * @param groupIds 权限组 ID 列表
     * @return 条目列表
     * @history 1.00 2026-09-07 XieMingJie Created.
     */
    List<SysPermGroupItemEntity> selectByGroupIds(@Param("groupIds") List<Long> groupIds);
}
