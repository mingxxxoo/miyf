package cn.miyf.kitchen.bean.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 菜谱领域模型；食材/调味/步骤等结构化列表在领域层使用普通 Java 对象。
 *
 * @author XieMingJie
 * @since 2026-09-04 16:41
 */
public class Recipe {
    private Long id;
    private Long dishId;
    private String description;
    private String difficulty;
    private Integer prepareMinutes;
    private Integer cookMinutes;
    private Integer servings;
    private List<Map<String, Object>> ingredients = new ArrayList<>();
    private List<Map<String, Object>> seasonings = new ArrayList<>();
    private List<Map<String, Object>> steps = new ArrayList<>();
    private String tips;
    private Map<String, Object> nutrition;
    private Instant createTime;
    private Instant lastModifyTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDishId() {
        return dishId;
    }

    public void setDishId(Long dishId) {
        this.dishId = dishId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getPrepareMinutes() {
        return prepareMinutes;
    }

    public void setPrepareMinutes(Integer prepareMinutes) {
        this.prepareMinutes = prepareMinutes;
    }

    public Integer getCookMinutes() {
        return cookMinutes;
    }

    public void setCookMinutes(Integer cookMinutes) {
        this.cookMinutes = cookMinutes;
    }

    public Integer getServings() {
        return servings;
    }

    public void setServings(Integer servings) {
        this.servings = servings;
    }

    public List<Map<String, Object>> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<Map<String, Object>> ingredients) {
        this.ingredients = ingredients;
    }

    public List<Map<String, Object>> getSeasonings() {
        return seasonings;
    }

    public void setSeasonings(List<Map<String, Object>> seasonings) {
        this.seasonings = seasonings;
    }

    public List<Map<String, Object>> getSteps() {
        return steps;
    }

    public void setSteps(List<Map<String, Object>> steps) {
        this.steps = steps;
    }

    public String getTips() {
        return tips;
    }

    public void setTips(String tips) {
        this.tips = tips;
    }

    public Map<String, Object> getNutrition() {
        return nutrition;
    }

    public void setNutrition(Map<String, Object> nutrition) {
        this.nutrition = nutrition;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Instant createTime) {
        this.createTime = createTime;
    }

    public Instant getLastModifyTime() {
        return lastModifyTime;
    }

    public void setLastModifyTime(Instant lastModifyTime) {
        this.lastModifyTime = lastModifyTime;
    }
}
