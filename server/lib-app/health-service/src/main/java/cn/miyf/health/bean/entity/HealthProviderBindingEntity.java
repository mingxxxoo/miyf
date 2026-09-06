package cn.miyf.health.bean.entity;

import cn.miyf.bean.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
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

/**
 * 主体与外部数据源账号绑定。
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
@TableName("health_provider_binding")
@Schema(name = "HealthProviderBindingEntity", description = "健康数据源绑定")
public class HealthProviderBindingEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("subject_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "主体 ID", type = "string")
    private Long subjectId;

    @TableField("provider_code")
    @Schema(description = "数据源编码")
    private String providerCode;

    @TableField("external_account_id")
    @Schema(description = "外部账号 ID")
    private String externalAccountId;

    @TableField("credential_ref")
    @Schema(description = "凭证引用（不落明文密钥）")
    private String credentialRef;

    @TableField("status")
    @Schema(description = "ACTIVE/INACTIVE/REVOKED")
    private String status;

    @TableField("last_sync_at")
    @Schema(description = "最近同步时间")
    private Instant lastSyncAt;
}
