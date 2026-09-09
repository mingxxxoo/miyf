package cn.miyf.permission.repository.mapper;

import cn.miyf.permission.bean.entity.SysRoleEntity;
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
     * 按角色码查询（全局唯一角色如 SUPER_ADMIN；同码多产品时取一条）。
     *
     * @param code 角色码
     * @return 角色
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    SysRoleEntity selectByCode(@Param("code") String code);

    /**
     * 按产品域 + 角色码查询。
     *
     * @param product 产品域
     * @param code    角色码
     * @return 角色，不存在则 null
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    SysRoleEntity selectByProductAndCode(@Param("product") String product, @Param("code") String code);

    /**
     * 查询用户已绑定角色。
     *
     * @param userId 用户 ID
     * @return 角色列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<SysRoleEntity> findByUserId(@Param("userId") Long userId);
}
