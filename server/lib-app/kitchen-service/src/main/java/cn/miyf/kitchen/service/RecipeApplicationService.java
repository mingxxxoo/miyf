package cn.miyf.kitchen.service;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.kitchen.bean.dto.RecipeMaterialDto;
import cn.miyf.kitchen.bean.dto.RecipeSaveDto;
import cn.miyf.kitchen.bean.dto.RecipeStepDto;
import cn.miyf.kitchen.bean.entity.DishEntity;
import cn.miyf.kitchen.bean.entity.RecipeEntity;
import cn.miyf.kitchen.bean.model.Dish;
import cn.miyf.kitchen.bean.model.Recipe;
import cn.miyf.kitchen.bean.vo.RecipeVo;
import cn.miyf.kitchen.helper.EntityConverters;
import cn.miyf.kitchen.repository.DishRepository;
import cn.miyf.kitchen.repository.RecipeRepository;
import cn.miyf.service.BaseApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 菜谱应用服务：一菜一谱、结构校验、管理端 CRUD、用户端只读。
 *
 * @author XieMingJie
 * @since 2026-09-04 17:35
 */
@Service
@RequiredArgsConstructor
public class RecipeApplicationService extends BaseApplicationService {

    private final RecipeRepository recipeRepository;
    private final DishRepository dishRepository;

    /**
     * 管理端菜谱列表。
     *
     * @return 菜谱列表
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    public List<RecipeVo> listAll() {
        return recipeRepository.selectAll().stream()
                .map(EntityConverters::toRecipe)
                .map(this::toVo)
                .toList();
    }

    /**
     * 管理端按 ID 查询。
     *
     * @param id 菜谱 ID
     * @return 菜谱
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    public RecipeVo getById(Long id) {
        return toVo(EntityConverters.toRecipe(requireById(recipeRepository, id, "菜谱不存在")));
    }

    /**
     * 管理端按菜品查询菜谱列表（0 或 1 条）。
     *
     * @param dishId 菜品 ID
     * @return 菜谱列表
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    public List<RecipeVo> listByDishId(Long dishId) {
        requireById(dishRepository, dishId, "菜品不存在");
        return findRecipeByDishId(dishId).map(r -> List.of(toVo(r))).orElse(List.of());
    }

    /**
     * 按菜品查询菜谱（管理端，不校验上架）。
     *
     * @param dishId 菜品 ID
     * @return 菜谱
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    public RecipeVo getByDishIdAdmin(Long dishId) {
        requireById(dishRepository, dishId, "菜品不存在");
        Recipe recipe = requireFound(findRecipeByDishId(dishId), "该菜品尚未配置菜谱");
        return toVo(recipe);
    }

    /**
     * 用户端按菜品查询菜谱：菜品须已上架。
     *
     * @param dishId 菜品 ID
     * @return 菜谱
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    public RecipeVo getByDishIdUser(Long dishId) {
        Dish dish = EntityConverters.toDish(requireById(dishRepository, dishId, "菜品不存在"));
        requireTrue("ON_SALE".equals(dish.getStatus()), ErrorCode.NOT_FOUND, "菜品不存在或未上架");
        Recipe recipe = requireFound(findRecipeByDishId(dishId), "该菜品暂无菜谱");
        return toVo(recipe);
    }

    /**
     * 创建菜谱（一菜一谱）。
     *
     * @param dto 请求
     * @return 新建菜谱
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Transactional
    public RecipeVo create(RecipeSaveDto dto) {
        RecipeStructureValidator.validateAndNormalize(dto);
        requireById(dishRepository, dto.getDishId(), "菜品不存在");
        if (findRecipeByDishId(dto.getDishId()).isPresent()) {
            throw new BusinessException(ErrorCode.CONFLICT, "该菜品已有菜谱，请直接更新");
        }
        Recipe recipe = new Recipe();
        applyDto(recipe, dto);
        RecipeEntity entity = EntityConverters.toRecipeEntity(recipe);
        insert(recipeRepository, entity);
        return toVo(EntityConverters.toRecipe(entity));
    }

    /**
     * 更新菜谱；不允许更换 dishId。
     *
     * @param id  菜谱 ID
     * @param dto 请求
     * @return 更新后菜谱
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Transactional
    public RecipeVo update(Long id, RecipeSaveDto dto) {
        RecipeStructureValidator.validateAndNormalize(dto);
        Recipe recipe = EntityConverters.toRecipe(requireById(recipeRepository, id, "菜谱不存在"));
        requireTrue(recipe.getDishId().equals(dto.getDishId()),
                ErrorCode.BAD_REQUEST, "不允许更换菜谱所属菜品");
        requireById(dishRepository, dto.getDishId(), "菜品不存在");
        applyDto(recipe, dto);
        RecipeEntity entity = EntityConverters.toRecipeEntity(recipe);
        update(recipeRepository, entity);
        return toVo(EntityConverters.toRecipe(entity));
    }

    /**
     * 物理删除菜谱。
     *
     * @param id 菜谱 ID
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    @Transactional
    public void delete(Long id) {
        requireById(recipeRepository, id, "菜谱不存在");
        deleteById(recipeRepository, id);
    }

    private Optional<Recipe> findRecipeByDishId(Long dishId) {
        return Optional.ofNullable(EntityConverters.toRecipe(recipeRepository.selectByDishId(dishId)));
    }

    /**
     * 将 DTO 应用到领域对象。
     *
     * @param recipe 菜谱
     * @param dto    请求
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    private void applyDto(Recipe recipe, RecipeSaveDto dto) {
        recipe.setDishId(dto.getDishId());
        recipe.setDescription(dto.getDescription());
        recipe.setDifficulty(dto.getDifficulty());
        recipe.setPrepareMinutes(dto.getPrepareMinutes());
        recipe.setCookMinutes(dto.getCookMinutes());
        recipe.setServings(dto.getServings());
        recipe.setIngredients(toMaterialMaps(dto.getIngredients()));
        recipe.setSeasonings(toMaterialMaps(dto.getSeasonings()));
        recipe.setSteps(toStepMaps(dto.getSteps()));
        recipe.setTips(dto.getTips());
        recipe.setNutrition(dto.getNutrition());
    }

    private List<Map<String, Object>> toMaterialMaps(List<RecipeMaterialDto> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> list = new ArrayList<>(items.size());
        for (RecipeMaterialDto item : items) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", item.getName());
            map.put("amount", item.getAmount());
            list.add(map);
        }
        return list;
    }

    private List<Map<String, Object>> toStepMaps(List<RecipeStepDto> steps) {
        if (steps == null || steps.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> list = new ArrayList<>(steps.size());
        for (RecipeStepDto step : steps) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("step", step.getStep());
            if (step.getTitle() != null) {
                map.put("title", step.getTitle());
            }
            map.put("description", step.getDescription());
            if (step.getImage() != null) {
                map.put("image", step.getImage());
            }
            list.add(map);
        }
        return list;
    }

    /**
     * 领域转 VO，并尽量补齐菜品名。
     *
     * @param recipe 菜谱
     * @return VO
     * @history 1.00 2026-09-04 XieMingJie Created.
     */
    private RecipeVo toVo(Recipe recipe) {
        RecipeVo vo = new RecipeVo()
                .setId(recipe.getId())
                .setDishId(recipe.getDishId())
                .setDescription(recipe.getDescription())
                .setDifficulty(recipe.getDifficulty())
                .setPrepareMinutes(recipe.getPrepareMinutes())
                .setCookMinutes(recipe.getCookMinutes())
                .setServings(recipe.getServings())
                .setIngredients(recipe.getIngredients())
                .setSeasonings(recipe.getSeasonings())
                .setSteps(recipe.getSteps())
                .setTips(recipe.getTips())
                .setNutrition(recipe.getNutrition())
                .setLastModifyTime(recipe.getLastModifyTime());
        if (recipe.getDishId() != null) {
            DishEntity dish = dishRepository.selectById(recipe.getDishId());
            if (dish != null) {
                vo.setDishName(dish.getName());
            }
        }
        return vo;
    }
}
