package cn.miyf.job.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 定时任务启停状态（sys_job_state）。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("sys_job_state")
@Schema(name = "SysJobStateEntity", description = "定时任务启停状态")
public class SysJobStateEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "code", type = IdType.INPUT)
    @Schema(description = "任务编码")
    private String code;

    @TableField("enabled")
    @Schema(description = "是否启用")
    private Boolean enabled;

    @TableField("updated_at")
    @Schema(description = "更新时间")
    private Instant updatedAt;
}
