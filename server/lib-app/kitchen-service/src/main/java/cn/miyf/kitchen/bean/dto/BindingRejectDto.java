package cn.miyf.kitchen.bean.dto;

import cn.miyf.bean.dto.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 拒绝绑定请求。
 *
 * @author XieMingJie
 * @since 2026-09-15
 * @history 1.00 2026-09-15 XieMingJie Created.
 */
@Getter
@Setter
@Schema(name = "BindingRejectDto")
public class BindingRejectDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 拒绝原因（可空） */
    private String reason;
}
