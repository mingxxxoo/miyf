package cn.miyf.repository.mapper;

import cn.miyf.bean.entity.SysRolePermGroupEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色-权限组绑定 Mapper。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Mapper
public interface SysRolePermGroupMapper extends BaseMapper<SysRolePermGroupEntity> {

    /**
     * 清空角色的权限组绑定。
     *
     * @param roleId 角色 ID
     * @return 影响行数
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询角色是否已绑定某权限组。
     *
     * @param roleId  角色 ID
     * @param groupId 权限组 ID
     * @return 绑定记录
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    SysRolePermGroupEntity selectByRoleAndGroup(@Param("roleId") Long roleId,
                                                @Param("groupId") Long groupId);
}
