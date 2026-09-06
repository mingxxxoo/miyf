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
}
