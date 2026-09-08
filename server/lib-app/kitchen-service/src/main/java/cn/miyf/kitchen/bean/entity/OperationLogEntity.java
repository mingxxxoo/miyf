package cn.miyf.kitchen.bean.entity;

import cn.miyf.bean.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;

/**
 * 操作日志表实体。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:54
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("operation_logs")
@Schema(name = "OperationLogEntity", description = "操作日志")
public class OperationLogEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("operator_id")
    @Schema(description = "操作人 ID")
    private Long operatorId;

    @TableField("operator_name")
    @Schema(description = "操作人名称")
    private String operatorName;

    @TableField("operation_type")
    @Schema(description = "操作类型")
    private String operationType;

    @TableField("target_type")
    @Schema(description = "目标类型")
    private String targetType;

    @TableField("target_id")
    @Schema(description = "目标 ID")
    private String targetId;

    @TableField("request_ip")
    @Schema(description = "请求 IP")
    private String requestIp;

    @TableField("request_method")
    @Schema(description = "HTTP 方法")
    private String requestMethod;

    @TableField("request_uri")
    @Schema(description = "请求 URI")
    private String requestUri;

    @TableField("operation_detail")
    @Schema(description = "操作详情（已脱敏）")
    private String operationDetail;
}
