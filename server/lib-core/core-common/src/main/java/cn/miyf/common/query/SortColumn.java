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
 * 排序列。
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
@Schema(description = "排序列")
public class SortColumn implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "列名，仅允许字母数字下划线与点")
    private String column;

    @Schema(description = "排序方向")
    private SortType type;
}
