package cn.miyf.kitchen.bean.entity;

import cn.miyf.bean.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.List;

/**
 * 菜品表实体。
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
@TableName(value = "dish", autoResultMap = true)
@Schema(name = "DishEntity", description = "菜品")
public class DishEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("category_id")
    @Schema(description = "分类 ID")
    private Long categoryId;

    @TableField("kitchen_id")
    @Schema(description = "所属厨房")
    private Long kitchenId;

    @TableField("org_unit_id")
    @Schema(description = "所属组织单位（sys_org_unit），模板按组织共享")
    private Long orgUnitId;

    @TableField("name")
    @Schema(description = "菜品名称")
    private String name;

    @TableField("subtitle")
    @Schema(description = "副标题")
    private String subtitle;

    @TableField("description")
    @Schema(description = "简介")
    private String description;

    @TableField("cover_image")
    @Schema(description = "封面图 URL")
    private String coverImage;

    @TableField("status")
    @Schema(description = "状态：DRAFT/ON_SALE/OFF_SALE")
    private String status;

    @TableField("audit_status")
    @Schema(description = "DRAFT/PENDING_REVIEW/APPROVED/REJECTED")
    private String auditStatus;

    @TableField("process_instance_id")
    @Schema(description = "Flowable 流程实例")
    private String processInstanceId;

    @TableField("reject_reason")
    @Schema(description = "最近驳回原因")
    private String rejectReason;

    @TableField("sort_order")
    @Schema(description = "排序值")
    private Integer sortOrder;

    @TableField("is_recommend")
    @Schema(description = "是否推荐")
    private Boolean recommend;

    @TableField("stock")
    @Schema(description = "可提供份数（非商业库存）")
    private Integer stock;

    @TableField("stock_type")
    @Schema(description = "份数模式：LIMITED/UNLIMITED")
    private String stockType;

    @TableField("unit")
    @Schema(description = "单位，默认份")
    private String unit;

    @TableField("rating")
    @Schema(description = "平均评分，系统维护")
    private BigDecimal rating;

    @TableField("rating_count")
    @Schema(description = "有效评价数，系统维护")
    private Integer ratingCount;

    @TableField("created_by")
    @Schema(description = "创建人")
    private Long createdBy;

    @TableField("updated_by")
    @Schema(description = "更新人")
    private Long updatedBy;

    @TableField("is_template")
    @Schema(description = "是否菜品模板（候选菜）：TRUE 不进点菜/预约链路")
    private Boolean isTemplate;

    @TableField("template_status")
    @Schema(description = "模板状态：draft/pre_created/published/archived；非模板为空")
    private String templateStatus;

    @TableField(value = "tags", typeHandler = JacksonTypeHandler.class)
    @Schema(description = "标签 JSON")
    private List<String> tags;

    @TableField("source_type")
    @Schema(description = "来源：MANUAL/AI_TEXT/AI_IMAGE/AI_URL")
    private String sourceType;

    @TableField("source_url")
    @Schema(description = "来源网页地址")
    private String sourceUrl;

    @TableField("estimated_total_seconds")
    @Schema(description = "预计总时长（秒），由步骤时长汇总")
    private Integer estimatedTotalSeconds;

    @TableField(exist = false)
    @Schema(description = "历史完成做菜次数（模板列表聚合字段）")
    private Integer usageCount;
}
