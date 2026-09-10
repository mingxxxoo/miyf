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
 * 权限组展示（含绑定的权限 ID 列表）。
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
@Schema(name = "SysPermGroupVo", description = "权限组")
public class SysPermGroupVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "权限组 ID", type = "string")
    private Long id;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "产品域")
    private String product;

    @Schema(description = "权限域：PERSONAL / ORG / SUPER")
    private String scope;

    @Schema(description = "权限 ID 列表", type = "array")
    private List<String> permissionIds = new ArrayList<>();

    @Schema(description = "创建时间")
    private Instant createTime;

    @Schema(description = "更新时间")
    private Instant lastModifyTime;
}
