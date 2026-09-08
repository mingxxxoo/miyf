package cn.miyf.user.bean.vo;

import cn.miyf.bean.vo.BaseVo;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统用户展示（不含密码哈希）。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "SysUserVo", description = "系统用户（脱敏）")
public class SysUserVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "用户 ID", type = "string")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "所属单位 ID", type = "string")
    private Long orgUnitId;

    @Schema(description = "登录名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "状态：ENABLED/DISABLED")
    private String status;

    @Schema(description = "角色 ID 列表", type = "array")
    private List<String> roleIds = new ArrayList<>();

    @Schema(description = "创建时间")
    private Instant createTime;

    @Schema(description = "更新时间")
    private Instant lastModifyTime;
}
