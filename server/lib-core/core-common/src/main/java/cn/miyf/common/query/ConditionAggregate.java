package cn.miyf.common.query;

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
 * 聚合统计参数（预留）。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:54
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(description = "聚合统计参数")
public class ConditionAggregate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "聚合类型，如 sum/count/avg")
    private String type;

    @Schema(description = "字段名")
    private String field;
}
