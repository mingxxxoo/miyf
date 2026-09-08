package cn.miyf.permission.bean.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 人员授权汇总。
 *
 * @author XieMingJie
 * @since 2026-09-07
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "RoleAuthSummaryVo", description = "人员授权汇总")
public class RoleAuthSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "角色数")
    private Integer roleCount;

    @Schema(description = "被授权去重人数")
    private Integer userCount;
}
