package cn.miyf.kitchen.bean.dto;

import cn.miyf.bean.dto.BaseDto;
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
 * 小程序用户更新个人资料。
 *
 * @author XieMingJie
 * @since 2026-09-11
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Schema(name = "UserProfileSaveDto", description = "个人资料更新")
public class UserProfileSaveDto extends BaseDto {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 URL（本站 /r/{id}，或与 file-storage.base-url 同 host 的绝对地址）")
    private String avatarUrl;
}
