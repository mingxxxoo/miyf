package cn.miyf.kitchen.bean.vo;

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

/**
 * 操作日志展示 VO（字段名对齐管理端前端）。
 *
 * @author XieMingJie
 * @since 2026-09-05
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "OperationLogVo", description = "操作日志")
public class OperationLogVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "日志 ID", type = "string")
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "操作人 ID", type = "string")
    private Long operatorId;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "动作")
    private String action;

    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "模块")
    private String module;

    @Schema(description = "详情")
    private String detail;

    @Schema(description = "IP")
    private String ip;

    @Schema(description = "请求方法")
    private String requestMethod;

    @Schema(description = "请求路径")
    private String requestUri;

    @Schema(description = "创建时间")
    private Instant createTime;
}
