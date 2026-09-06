package cn.miyf.repository.mapper;

import cn.miyf.bean.entity.SysPermGroupItemEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
     * 清空权限组内条目。
     *
     * @param groupId 权限组 ID
     * @return 影响行数
     * @history 1.00 2026-09-05 XieMingJie Created.
     */
    int deleteByGroupId(@Param("groupId") Long groupId);
}
