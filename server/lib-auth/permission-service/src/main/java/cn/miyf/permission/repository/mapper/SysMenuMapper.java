package cn.miyf.permission.repository.mapper;

import cn.miyf.permission.bean.entity.SysMenuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 菜单 Mapper。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenuEntity> {

    /**
     * 查询全部启用且可见的菜单（按排序）。
     *
     * @return 菜单列表
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    List<SysMenuEntity> selectVisibleMenus();
}
