package cn.miyf.repository.mapper;

import cn.miyf.bean.entity.SysUserRoleEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户-角色绑定 Mapper。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRoleEntity> {

    /**
     * 清空用户角色绑定。
     *
     * @param userId 用户 ID
     * @return 影响行数
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 删除用户-角色绑定。
     *
     * @param userId 用户 ID
     * @param roleId 角色 ID
     * @return 影响行数
     * @history 1.00 2026-09-07 XieMingJie Created.
     */
    int deleteByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 统计角色下用户数。
     *
     * @param roleId 角色 ID
     * @return 人数
     * @history 1.00 2026-09-07 XieMingJie Created.
     */
    int countByRoleId(@Param("roleId") Long roleId);
}
