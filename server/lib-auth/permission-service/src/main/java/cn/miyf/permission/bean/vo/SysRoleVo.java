package cn.miyf.permission.bean.vo;

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
 * 角色展示（含默认标记、产品域、授权人数、权限组）。
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
@Schema(name = "SysRoleVo", description = "角色")
public class SysRoleVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "角色 ID", type = "string")
    private Long id;

    @Schema(description = "角色码")
    private String code;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "是否默认角色")
    private Boolean isDefault;

    @Schema(description = "产品域")
    private String product;

    @Schema(description = "数据范围：ALL / ORG / ORG_CHILD / SELF")
    private String dataScope;

    @Schema(description = "被授权人数")
    private Integer userCount;

    @Schema(description = "权限组 ID 列表", type = "array")
    private List<String> groupIds = new ArrayList<>();

    @Schema(description = "创建时间")
    private Instant createTime;

    @Schema(description = "更新时间")
    private Instant lastModifyTime;
}
