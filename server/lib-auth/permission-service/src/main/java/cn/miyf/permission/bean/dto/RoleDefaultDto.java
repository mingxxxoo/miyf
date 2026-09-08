package cn.miyf.permission.bean.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 设/取消默认角色。
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
@Schema(name = "RoleDefaultDto", description = "默认角色开关")
public class RoleDefaultDto {

    @JsonProperty("isDefault")
    @Schema(description = "是否设为默认；false 表示取消")
    private Boolean defaultFlag;

    public boolean isDefault() {
        return Boolean.TRUE.equals(defaultFlag);
    }
}
