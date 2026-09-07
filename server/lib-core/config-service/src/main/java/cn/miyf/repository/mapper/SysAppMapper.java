package cn.miyf.repository.mapper;

import cn.miyf.bean.entity.SysAppEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 系统应用 Mapper。
 *
 * @author XieMingJie
 * @since 2026-09-07
 */
@Mapper
public interface SysAppMapper extends BaseMapper<SysAppEntity> {

    /**
     * 按编码查询。
     *
     * @param code 编码
     * @return 实体
     * @history 1.00 2026-09-07 XieMingJie Created.
     */
    SysAppEntity selectByCode(@Param("code") String code);

    /**
     * 按 product 统计权限数量。
     *
     * @return 每行含 product、cnt
     * @history 1.00 2026-09-07 XieMingJie Created.
     */
    List<Map<String, Object>> countPermissionsByProduct();

    /**
     * 按 product 统计菜单数量（DIR/MENU）。
     *
     * @return 每行含 product、cnt
     * @history 1.00 2026-09-07 XieMingJie Created.
     */
    List<Map<String, Object>> countMenusByProduct();
}
