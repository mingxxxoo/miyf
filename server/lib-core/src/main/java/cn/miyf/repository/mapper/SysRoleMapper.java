package cn.miyf.repository.mapper;

import cn.miyf.bean.entity.SysRoleEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色 Mapper。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRoleEntity> {

    /**
     * 按角色码查询。
     *
     * @param code 角色码
     * @return 角色
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    SysRoleEntity selectByCode(@Param("code") String code);

    /**
     * 查询用户已绑定角色。
     *
     * @param userId 用户 ID
     * @return 角色列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<SysRoleEntity> findByUserId(@Param("userId") Long userId);
}
