package cn.miyf.repository.mapper;

import cn.miyf.bean.entity.SysUserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统用户 Mapper。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUserEntity> {

    /**
     * 按用户名查询（未删除）。
     *
     * @param username 登录名
     * @return 用户实体
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    SysUserEntity selectByUsername(@Param("username") String username);
}
