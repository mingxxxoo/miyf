package cn.miyf.oss.bean.vo;

import cn.miyf.bean.vo.BaseVo;
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
 * 文件上传结果 VO。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:19
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "UploadedFileVo", description = "上传结果")
public class UploadedFileVo extends BaseVo {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "对象键")
    private String objectKey;

    @Schema(description = "可访问 URL")
    private String url;

    @Schema(description = "Content-Type")
    private String contentType;

    @Schema(description = "字节大小")
    private Long size;
}
