package cn.miyf.kitchen.bean.dto;

import cn.miyf.bean.dto.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 申请绑定请求：邀请短码与 token 二选一，token 优先。
 *
 * @author XieMingJie
 * @since 2026-09-15
 * @history 1.00 2026-09-15 XieMingJie Created.
 */
@Getter
@Setter
@Schema(name = "BindingApplyDto")
public class BindingApplyDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 邀请短码 */
    private String code;
    /** 邀请 token（优先于短码） */
    private String token;
}
