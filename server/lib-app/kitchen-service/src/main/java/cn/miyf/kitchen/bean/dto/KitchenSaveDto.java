package cn.miyf.kitchen.bean.dto;

import cn.miyf.bean.dto.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * 创建/编辑厨房请求。
 *
 * @author XieMingJie
 * @since 2026-09-15
 */
@Getter
@Setter
@Schema(name = "KitchenSaveDto")
public class KitchenSaveDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 厨房名称 */
    @NotBlank
    private String name;
    /** 简介 */
    private String intro;
    /** 封面图 */
    private String coverImage;
    /** 厨师可改：OPEN/CLOSED */
    @Schema(description = "OPEN/CLOSED")
    private String status;
}
