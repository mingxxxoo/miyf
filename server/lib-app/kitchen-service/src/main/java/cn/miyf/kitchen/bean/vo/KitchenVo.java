package cn.miyf.kitchen.bean.vo;

import cn.miyf.bean.vo.BaseVo;
import cn.miyf.oss.security.FileAccess;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.time.Instant;

/**
 * 厨房展示 VO。
 *
 * @author XieMingJie
 * @since 2026-09-15
 * @history 1.00 2026-09-15 XieMingJie Created.
 */
@Getter
@Setter
@Accessors(chain = true)
@Schema(name = "KitchenVo")
public class KitchenVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long ownerUserId;
    private String name;
    private String intro;
    @FileAccess
    private String coverImage;
    /** OPEN/CLOSED/BANNED */
    private String status;
    private Instant createTime;
}
