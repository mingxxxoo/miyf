package cn.miyf.kitchen.bean.vo;

import cn.miyf.bean.vo.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.util.List;

/**
 * 厨师端微信订阅消息配置。
 *
 * @author XieMingJie
 * @since 2026-09-16
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "WxSubscribeConfigVo", description = "微信订阅消息配置")
public class WxSubscribeConfigVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "是否启用推送")
    private boolean enabled;

    @Schema(description = "需授权的模板 ID 列表")
    private List<String> templateIds;
}
