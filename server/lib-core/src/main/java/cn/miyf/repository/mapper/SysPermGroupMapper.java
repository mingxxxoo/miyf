package cn.miyf.repository.mapper;

import cn.miyf.bean.entity.SysPermGroupEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限组 Mapper。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Mapper
public interface SysPermGroupMapper extends BaseMapper<SysPermGroupEntity> {

    /**
     * 按编码查询。
     *
     * @param code 权限组编码
     * @return 权限组
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    SysPermGroupEntity selectByCode(@Param("code") String code);

    /**
     * 查询角色已绑定的权限组。
     *
     * @param roleId 角色 ID
     * @return 权限组列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<SysPermGroupEntity> findByRoleId(@Param("roleId") Long roleId);
}
