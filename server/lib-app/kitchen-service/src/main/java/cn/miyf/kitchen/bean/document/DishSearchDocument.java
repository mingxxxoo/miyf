package cn.miyf.kitchen.bean.document;

import cn.miyf.infrastructure.search.SearchFieldMapping;
import cn.miyf.kitchen.bean.model.Dish;

import java.math.BigDecimal;
import java.util.List;

/**
 * 上架菜品 ES 文档（仅索引 ON_SALE；下架/删除时物理移除）。
 * 索引名、字段映射与默认排序与本 Document 绑定，供召回与写入共用。
 *
 * @author XieMingJie
 * @since 2026-09-08
 */
public class DishSearchDocument {

    /** 逻辑索引名（物理名由 app.search.index-prefix 拼接）。 */
    public static final String INDEX = "kitchen_dish";

    /** 用户端列表默认排序（与 SQL @conditionSql 默认一致）。 */
    public static final String DEFAULT_ORDER =
            "d.is_recommend DESC, d.sort_order ASC, d.create_time DESC";

    /** 热门列表排序。 */
    public static final String HOT_ORDER =
            "d.rating DESC, d.rating_count DESC, d.sort_order ASC";

    /** 推荐列表排序。 */
    public static final String RECOMMEND_ORDER =
            "d.sort_order ASC, d.create_time DESC";

    /** multi_match 字段（可含权重）。 */
    public static final List<String> KEYWORD_FIELDS = List.of(
            "name^3", "subtitle^2", "description", "categoryName");

    /** SQL 列 → ES 字段。 */
    public static final SearchFieldMapping FIELD_MAPPING = SearchFieldMapping.create()
            .map("is_recommend", "recommend")
            .map("d.is_recommend", "recommend")
            .map("sort_order", "sortOrder")
            .map("d.sort_order", "sortOrder")
            .map("create_time", "createTimeEpochMs")
            .map("d.create_time", "createTimeEpochMs")
            .map("rating", "rating")
            .map("d.rating", "rating")
            .map("rating_count", "ratingCount")
            .map("d.rating_count", "ratingCount")
            .map("name", "name")
            .map("d.name", "name");

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String subtitle;
    private String description;
    private String coverImage;
    private boolean recommend;
    private int sortOrder;
    private BigDecimal rating;
    private int ratingCount;
    /** 创建时间 epoch 毫秒，便于排序。 */
    private long createTimeEpochMs;

    /**
     * 由领域模型构造文档。
     *
     * @param dish 菜品
     * @return 文档；dish 为空则 null
     * @history 1.00 2026-09-08 XieMingJie Created.
     */
    public static DishSearchDocument from(Dish dish) {
        if (dish == null) {
            return null;
        }
        long createMs = dish.getCreateTime() == null ? 0L : dish.getCreateTime().toEpochMilli();
        BigDecimal rating = dish.getRating() == null ? BigDecimal.ZERO : dish.getRating();
        return new DishSearchDocument()
                .setId(dish.getId())
                .setCategoryId(dish.getCategoryId())
                .setCategoryName(dish.getCategoryName())
                .setName(dish.getName())
                .setSubtitle(dish.getSubtitle())
                .setDescription(dish.getDescription())
                .setCoverImage(dish.getCoverImage())
                .setRecommend(dish.isRecommend())
                .setSortOrder(dish.getSortOrder())
                .setRating(rating)
                .setRatingCount(dish.getRatingCount())
                .setCreateTimeEpochMs(createMs);
    }

    public Long getId() {
        return id;
    }

    public DishSearchDocument setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public DishSearchDocument setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
        return this;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public DishSearchDocument setCategoryName(String categoryName) {
        this.categoryName = categoryName;
        return this;
    }

    public String getName() {
        return name;
    }

    public DishSearchDocument setName(String name) {
        this.name = name;
        return this;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public DishSearchDocument setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public DishSearchDocument setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public DishSearchDocument setCoverImage(String coverImage) {
        this.coverImage = coverImage;
        return this;
    }

    public boolean isRecommend() {
        return recommend;
    }

    public DishSearchDocument setRecommend(boolean recommend) {
        this.recommend = recommend;
        return this;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public DishSearchDocument setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public DishSearchDocument setRating(BigDecimal rating) {
        this.rating = rating;
        return this;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public DishSearchDocument setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
        return this;
    }

    public long getCreateTimeEpochMs() {
        return createTimeEpochMs;
    }

    public DishSearchDocument setCreateTimeEpochMs(long createTimeEpochMs) {
        this.createTimeEpochMs = createTimeEpochMs;
        return this;
    }
}
