package cn.miyf.health.bean.vo;

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

import java.time.Instant;

/**
 * 健康数据源绑定对外视图。
 * 不返回 credentialRef；外部账号脱敏，仅暴露是否已配置凭证。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "HealthProviderBindingVo", description = "健康数据源绑定（脱敏）")
public class HealthProviderBindingVo {

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主键", type = "string")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主体 ID", type = "string")
    private Long subjectId;

    @Schema(description = "数据源编码")
    private String providerCode;

    @Schema(description = "数据源显示名")
    private String providerDisplayName;

    @Schema(description = "外部账号（脱敏）")
    private String externalAccountMasked;

    @Schema(description = "是否已配置凭证引用")
    private boolean hasCredential;

    @Schema(description = "绑定状态")
    private String status;

    @Schema(description = "最近同步时间")
    private Instant lastSyncTime;
}
