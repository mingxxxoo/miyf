package cn.miyf.notification.repository.mapper;

import cn.miyf.notification.entity.SysNotifyTemplateEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 通知模板 Mapper。
 *
 * @author XieMingJie
 * @since 2026-09-07
 */
@Mapper
public interface SysNotifyTemplateMapper extends BaseMapper<SysNotifyTemplateEntity> {

    SysNotifyTemplateEntity selectByCode(@Param("code") String code);
}
