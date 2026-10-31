package cn.miyf.kitchen.service;

import cn.miyf.auth.security.SecurityUtils;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.common.PageResult;
import cn.miyf.kitchen.bean.dto.DishTemplateSaveDto;
import cn.miyf.kitchen.bean.dto.RecipeMaterialDto;
import cn.miyf.kitchen.bean.dto.RecipeSaveDto;
import cn.miyf.kitchen.bean.dto.RecipeStepDto;
import cn.miyf.kitchen.bean.entity.DishEntity;
import cn.miyf.kitchen.bean.entity.DishImageEntity;
import cn.miyf.kitchen.bean.entity.KitchenEntity;
import cn.miyf.kitchen.bean.entity.RecipeEntity;
import cn.miyf.kitchen.bean.entity.UserEntity;
import cn.miyf.kitchen.bean.model.template.DishTemplateStatus;
import cn.miyf.kitchen.bean.model.template.DishTemplateStatusMachine;
import cn.miyf.kitchen.bean.qo.DishTemplatePageQo;
import cn.miyf.kitchen.bean.vo.DishTemplateVo;
import cn.miyf.kitchen.repository.DishImageRepository;
import cn.miyf.kitchen.repository.DishRepository;
import cn.miyf.kitchen.repository.RecipeRepository;
import cn.miyf.kitchen.repository.UserRepository;
import cn.miyf.oss.bean.dto.FileUploadCommand;
import cn.miyf.oss.bean.vo.UploadedFileVo;
import cn.miyf.oss.enums.FileAccessPermission;
import cn.miyf.oss.service.FileResourceApplicationService;
import cn.miyf.service.BaseApplicationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 菜品模板（候选菜）应用服务。
 * <p>
 * 模板仍存放于 {@code dish}，通过 {@code is_template} 与 {@code template_status} 区分，
 * 不进入点菜/预约链路；原材料与步骤复用 {@code dish_recipe} 的 JSONB 结构。
 * 全厨房成员可创建、编辑、发布与归档模板，删除仅限创建人或厨房主人。
 *
 * @author XieMingJie
 * @since 2026-10-31 12:00
 * @history 1.00 2026-10-31 12:00 XieMingJie Created.
 */
@Service
@RequiredArgsConstructor
public class DishTemplateApplicationService extends BaseApplicationService {

    private static final Set<String> SOURCE_TYPES = Set.of("MANUAL", "AI_TEXT", "AI_IMAGE", "AI_URL");
    private static final Set<String> SAVE_STATUSES = Set.of(
            DishTemplateStatus.DRAFT, DishTemplateStatus.PRE_CREATED);
    private static final int MAX_TAGS = 10;
    private static final int MAX_TAG_LENGTH = 16;

    private final DishRepository dishRepository;
    private final DishImageRepository dishImageRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final KitchenAccessService kitchenAccessService;
    private final FileResourceApplicationService fileResourceApplicationService;

    /**
     * 上传菜品模板封面/步骤图片。
     * <p>
     * 组织成员均可上传，文件公开可读，便于同组织成员查看菜品。
     *
     * @param file 图片文件
     * @return 上传结果（含 /r/{id}）
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    public UploadedFileVo uploadImage(MultipartFile file) {
        kitchenAccessService.requireMemberKitchen();
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "请选择图片");
        }
        FileUploadCommand command = new FileUploadCommand()
                .setAppCode("kitchen")
                .setSource("dish-template")
                .setTemp(false)
                .setCompress(true)
                .setAccessPermission(FileAccessPermission.PUBLIC);
        try (InputStream in = file.getInputStream()) {
            return fileResourceApplicationService.store(
                    in,
                    file.getSize(),
                    file.getContentType(),
                    file.getOriginalFilename(),
                    command
            );
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE, "读取图片失败");
        }
    }

    /**
     * 菜品库分页：默认不含已归档，支持状态、我创建的、最近使用筛选。
     *
     * @param qo 查询条件
     * @return 分页结果
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    public PageResult<DishTemplateVo> page(DishTemplatePageQo qo) {
        KitchenEntity kitchen = kitchenAccessService.requireMemberKitchen();
        Long orgUnitId = kitchenAccessService.requireOrgUnitId(kitchen);
        Long currentUserId = SecurityUtils.currentUserId();
        String status = normalizeStatusFilter(qo.getStatus());
        Long creatorId = Boolean.TRUE.equals(qo.getMine()) ? currentUserId : null;
        Long recentUserId = Boolean.TRUE.equals(qo.getRecent()) ? currentUserId : null;
        long page = pageOf(qo);
        long pageSize = pageSizeOf(qo);
        long offset = offset(page, pageSize);

        List<DishEntity> entities = dishRepository.selectTemplatePage(
                orgUnitId, status, creatorId, recentUserId, qo.getMade(), qo.getKeyword(), offset, pageSize);
        long total = dishRepository.countTemplatePage(
                orgUnitId, status, creatorId, recentUserId, qo.getMade(), qo.getKeyword());
        if (entities.isEmpty()) {
            return emptyPage(page, pageSize);
        }
        return PageResult.of(toVoList(entities), total, page, pageSize);
    }

    /**
     * 模板详情：含原材料、步骤与当前用户可执行动作。
     *
     * @param id 菜品模板 ID
     * @return 模板 VO
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    public DishTemplateVo detail(Long id) {
        KitchenEntity kitchen = kitchenAccessService.requireMemberKitchen();
        Long orgUnitId = kitchenAccessService.requireOrgUnitId(kitchen);
        DishEntity dish = requireTemplate(id, orgUnitId, kitchen.getId());
        RecipeEntity recipe = recipeRepository.selectByDishId(id);
        UserEntity creator = dish.getCreatedBy() == null ? null : userRepository.selectById(dish.getCreatedBy());
        return toVo(dish, recipe, creator);
    }

    /**
     * 创建模板：默认草稿，可保存为预创建；服务端重排步骤并汇总总时长。
     *
     * @param dto 保存请求
     * @return 新建模板 ID
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    @Transactional
    public Long create(DishTemplateSaveDto dto) {
        KitchenEntity kitchen = kitchenAccessService.requireMemberKitchen();
        Long orgUnitId = kitchenAccessService.requireOrgUnitId(kitchen);
        Long currentUserId = SecurityUtils.currentUserId();
        RecipeSaveDto recipeDto = normalize(dto);
        String targetStatus = normalizeTargetStatus(dto.getTargetStatus());

        DishEntity dish = new DishEntity();
        dish.setKitchenId(kitchen.getId());
        dish.setOrgUnitId(orgUnitId);
        dish.setName(dto.getName().trim());
        dish.setSubtitle(null);
        dish.setDescription(recipeDto.getDescription());
        dish.setCoverImage(trimOptional(dto.getCoverImage(), 512, "封面图"));
        dish.setTags(normalizeTags(dto.getTags()));
        dish.setSourceType(normalizeSourceType(dto.getSourceType()));
        dish.setSourceUrl(normalizeSourceUrl(dto.getSourceUrl()));
        dish.setEstimatedTotalSeconds(sumDurations(recipeDto.getSteps()));
        dish.setIsTemplate(Boolean.TRUE);
        dish.setTemplateStatus(targetStatus);
        // 模板不参与点菜：菜单状态固定草稿，审核状态视为通过，避免进入审核待办
        dish.setStatus("DRAFT");
        dish.setAuditStatus("APPROVED");
        dish.setRecommend(Boolean.FALSE);
        dish.setSortOrder(0);
        dish.setStock(0);
        dish.setStockType("UNLIMITED");
        dish.setUnit("份");
        dish.setCreatedBy(currentUserId);
        dish.setUpdatedBy(currentUserId);
        insert(dishRepository, dish);

        saveRecipe(dish.getId(), recipeDto);
        return dish.getId();
    }

    /**
     * 更新模板：模板内容与状态可同时调整；已归档模板需先恢复为草稿。
     *
     * @param id  模板 ID
     * @param dto 保存请求
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    @Transactional
    public void update(Long id, DishTemplateSaveDto dto) {
        KitchenEntity kitchen = kitchenAccessService.requireMemberKitchen();
        Long orgUnitId = kitchenAccessService.requireOrgUnitId(kitchen);
        DishEntity dish = requireTemplate(id, orgUnitId, kitchen.getId());
        RecipeSaveDto recipeDto = normalize(dto);
        String from = dish.getTemplateStatus();
        String target = normalizeOptionalStatus(dto.getTargetStatus());
        if (target != null && !target.equals(from)) {
            DishTemplateStatusMachine.assertTransit(from, target);
            dish.setTemplateStatus(target);
        } else {
            requireTrue(!DishTemplateStatus.ARCHIVED.equals(from),
                    ErrorCode.INVALID_STATUS, "已归档模板请先恢复为草稿");
        }

        dish.setName(dto.getName().trim());
        dish.setDescription(recipeDto.getDescription());
        dish.setCoverImage(trimOptional(dto.getCoverImage(), 512, "封面图"));
        dish.setTags(normalizeTags(dto.getTags()));
        dish.setSourceType(normalizeSourceType(dto.getSourceType()));
        dish.setSourceUrl(normalizeSourceUrl(dto.getSourceUrl()));
        dish.setEstimatedTotalSeconds(sumDurations(recipeDto.getSteps()));
        dish.setUpdatedBy(SecurityUtils.currentUserId());
        update(dishRepository, dish);

        saveRecipe(id, recipeDto);
    }

    /**
     * 发布模板：draft/pre_created → published，要求至少一个步骤。
     * <p>
     * 采用状态 CAS 更新，避免并发发布/归档互相覆盖。
     *
     * @param id 模板 ID
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    @Transactional
    public void publish(Long id) {
        KitchenEntity kitchen = kitchenAccessService.requireMemberKitchen();
        Long orgUnitId = kitchenAccessService.requireOrgUnitId(kitchen);
        DishEntity dish = requireTemplate(id, orgUnitId, kitchen.getId());
        RecipeEntity recipe = recipeRepository.selectByDishId(id);
        requireTrue(recipe != null && recipe.getSteps() != null && !recipe.getSteps().isEmpty(),
                ErrorCode.BAD_REQUEST, "请先配置至少一个做菜步骤");
        String from = dish.getTemplateStatus();
        DishTemplateStatusMachine.assertTransit(from, DishTemplateStatus.PUBLISHED);
        int rows = dishRepository.updateTemplateStatus(id, from, DishTemplateStatus.PUBLISHED);
        requireTrue(rows > 0, ErrorCode.CONFLICT, "模板状态已变化，请刷新后重试");
    }

    /**
     * 归档模板：任意未归档状态 → archived，已创建任务不受影响（快照隔离）。
     *
     * @param id 模板 ID
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    @Transactional
    public void archive(Long id) {
        KitchenEntity kitchen = kitchenAccessService.requireMemberKitchen();
        Long orgUnitId = kitchenAccessService.requireOrgUnitId(kitchen);
        DishEntity dish = requireTemplate(id, orgUnitId, kitchen.getId());
        String from = dish.getTemplateStatus();
        DishTemplateStatusMachine.assertTransit(from, DishTemplateStatus.ARCHIVED);
        int rows = dishRepository.updateTemplateStatus(id, from, DishTemplateStatus.ARCHIVED);
        requireTrue(rows > 0, ErrorCode.CONFLICT, "模板状态已变化，请刷新后重试");
    }

    /**
     * 删除模板：仅草稿/预创建且未产生做菜任务时允许，删除人须为创建人或厨房主人。
     *
     * @param id 模板 ID
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    @Transactional
    public void delete(Long id) {
        KitchenEntity kitchen = kitchenAccessService.requireMemberKitchen();
        Long orgUnitId = kitchenAccessService.requireOrgUnitId(kitchen);
        DishEntity dish = requireTemplate(id, orgUnitId, kitchen.getId());
        Long currentUserId = SecurityUtils.currentUserId();
        boolean sameKitchenOwner = Objects.equals(kitchen.getId(), dish.getKitchenId())
                && kitchenAccessService.isOwner(kitchen);
        boolean allowed = Objects.equals(currentUserId, dish.getCreatedBy()) || sameKitchenOwner;
        requireTrue(allowed, ErrorCode.FORBIDDEN, "仅创建人或厨房主人可删除菜品模板");
        requireTrue(DishTemplateStatusMachine.editable(dish.getTemplateStatus()),
                ErrorCode.INVALID_STATUS, "仅草稿或预创建状态可删除，已发布请先归档");
        requireTrue(dishRepository.countCookingTasks(id) == 0,
                ErrorCode.CONFLICT, "该菜品已有做菜记录，请改为归档");

        RecipeEntity recipe = recipeRepository.selectByDishId(id);
        if (recipe != null) {
            deleteById(recipeRepository, recipe.getId());
        }
        dishImageRepository.delete(new LambdaQueryWrapper<DishImageEntity>()
                .eq(DishImageEntity::getDishId, id));
        deleteById(dishRepository, id);
    }

    /**
     * 读取并校验模板归属；非模板或跨组织一律拒绝。
     *
     * @param id         模板 ID
     * @param orgUnitId  当前用户组织单元 ID
     * @param kitchenId  当前用户厨房 ID，用于兼容未回填组织的历史模板
     * @return 模板实体
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    private DishEntity requireTemplate(Long id, Long orgUnitId, Long kitchenId) {
        DishEntity dish = requireById(dishRepository, id, "菜品模板不存在");
        requireTrue(Boolean.TRUE.equals(dish.getIsTemplate()),
                ErrorCode.NOT_FOUND, "菜品模板不存在");
        boolean sameOrg = orgUnitId != null && orgUnitId.equals(dish.getOrgUnitId());
        boolean legacySameKitchen = dish.getOrgUnitId() == null && kitchenId.equals(dish.getKitchenId());
        requireTrue(sameOrg || legacySameKitchen,
                ErrorCode.FORBIDDEN, "无权访问该菜品模板");
        return dish;
    }

    /**
     * 规范化保存请求：复用菜谱结构校验（原材料、步骤、时长、计时方式）。
     *
     * @param dto 保存请求
     * @return 规范化后的菜谱请求
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    private RecipeSaveDto normalize(DishTemplateSaveDto dto) {
        RecipeSaveDto recipeDto = new RecipeSaveDto()
                .setDescription(trimOptional(dto.getDescription(), 2000, "菜品介绍"))
                .setDifficulty(dto.getDifficulty())
                .setServings(dto.getServings())
                .setIngredients(dto.getIngredients())
                .setSeasonings(List.of())
                .setSteps(dto.getSteps());
        RecipeStructureValidator.validateAndNormalize(recipeDto);
        if (recipeDto.getServings() == null) {
            recipeDto.setServings(1);
        }
        return recipeDto;
    }

    private String normalizeTargetStatus(String status) {
        String normalized = status == null || status.isBlank()
                ? DishTemplateStatus.DRAFT
                : DishTemplateStatus.normalize(status);
        requireTrue(SAVE_STATUSES.contains(normalized), ErrorCode.BAD_REQUEST,
                "保存状态仅支持 draft 或 pre_created");
        return normalized;
    }

    private String normalizeOptionalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = DishTemplateStatus.normalize(status);
        requireTrue(DishTemplateStatus.isValid(normalized), ErrorCode.BAD_REQUEST, "模板状态不合法");
        return normalized;
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = DishTemplateStatus.normalize(status);
        requireTrue(DishTemplateStatus.isValid(normalized), ErrorCode.BAD_REQUEST, "模板状态不合法");
        return normalized;
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag == null) {
                continue;
            }
            String trimmed = tag.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            requireTrue(trimmed.length() <= MAX_TAG_LENGTH, ErrorCode.BAD_REQUEST,
                    "单个标签不能超过 " + MAX_TAG_LENGTH + " 个字符");
            unique.add(trimmed);
        }
        requireTrue(unique.size() <= MAX_TAGS, ErrorCode.BAD_REQUEST,
                "标签最多 " + MAX_TAGS + " 个");
        return new ArrayList<>(unique);
    }

    private String normalizeSourceType(String sourceType) {
        String normalized = sourceType == null || sourceType.isBlank()
                ? "MANUAL"
                : sourceType.trim().toUpperCase(Locale.ROOT);
        requireTrue(SOURCE_TYPES.contains(normalized), ErrorCode.BAD_REQUEST,
                "来源仅支持 MANUAL/AI_TEXT/AI_IMAGE/AI_URL");
        return normalized;
    }

    private String normalizeSourceUrl(String sourceUrl) {
        String trimmed = trimOptional(sourceUrl, 1024, "来源地址");
        if (trimmed == null) {
            return null;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        requireTrue(lower.startsWith("http://") || lower.startsWith("https://"),
                ErrorCode.BAD_REQUEST, "来源地址仅支持 http/https");
        return trimmed;
    }

    private String trimOptional(String value, int max, String label) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        requireTrue(trimmed.length() <= max, ErrorCode.BAD_REQUEST,
                label + "不能超过 " + max + " 个字符");
        return trimmed;
    }

    /**
     * 汇总步骤时长（秒）；无任何时长时返回 null，表示未估算。
     *
     * @param steps 步骤
     * @return 预计总时长
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    private Integer sumDurations(List<RecipeStepDto> steps) {
        if (steps == null || steps.isEmpty()) {
            return null;
        }
        int total = 0;
        for (RecipeStepDto step : steps) {
            if (step.getDurationSeconds() != null && step.getDurationSeconds() > 0) {
                total += step.getDurationSeconds();
            }
        }
        return total > 0 ? total : null;
    }

    /**
     * 一菜一谱：存在则更新，不存在则新建。
     *
     * @param dishId 菜品 ID
     * @param dto    规范化后的菜谱请求
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    private void saveRecipe(Long dishId, RecipeSaveDto dto) {
        RecipeEntity entity = recipeRepository.selectByDishId(dishId);
        boolean creating = entity == null;
        if (creating) {
            entity = new RecipeEntity();
            entity.setDishId(dishId);
        }
        entity.setDescription(dto.getDescription());
        entity.setDifficulty(dto.getDifficulty());
        entity.setServings(dto.getServings());
        entity.setPrepareMinutes(dto.getPrepareMinutes());
        entity.setCookMinutes(dto.getCookMinutes());
        entity.setIngredients(toMaterialMaps(dto.getIngredients()));
        entity.setSeasonings(toMaterialMaps(dto.getSeasonings()));
        entity.setSteps(toStepMaps(dto.getSteps()));
        entity.setTips(dto.getTips());
        entity.setNutrition(dto.getNutrition());
        if (creating) {
            insert(recipeRepository, entity);
        } else {
            update(recipeRepository, entity);
        }
    }

    private List<Map<String, Object>> toMaterialMaps(List<RecipeMaterialDto> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> list = new ArrayList<>(items.size());
        for (RecipeMaterialDto item : items) {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("name", item.getName());
            map.put("amount", item.getAmount());
            map.put("unit", item.getUnit());
            map.put("processing", item.getProcessing());
            map.put("required", item.getRequired() == null || item.getRequired());
            map.put("substitute", item.getSubstitute());
            map.put("note", item.getNote());
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
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("order", step.getOrder());
            map.put("step", step.getStep());
            map.put("title", step.getTitle());
            map.put("description", step.getDescription());
            map.put("image", step.getImage());
            map.put("video", step.getVideo());
            map.put("durationSeconds", step.getDurationSeconds());
            map.put("timerType", step.getTimerType());
            map.put("parallelGroup", step.getParallelGroup());
            map.put("completionHint", step.getCompletionHint());
            map.put("warning", step.getWarning());
            map.put("required", step.getRequired() == null || step.getRequired());
            map.put("canUndo", step.getCanUndo() == null || step.getCanUndo());
            list.add(map);
        }
        return list;
    }

    /**
     * 批量组装 VO：一次取菜谱与创建人，避免 N+1。
     *
     * @param entities 模板实体
     * @return VO 列表
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    private List<DishTemplateVo> toVoList(List<DishEntity> entities) {
        List<Long> dishIds = entities.stream().map(DishEntity::getId).toList();
        Map<Long, RecipeEntity> recipeByDish = recipeRepository.selectList(
                        new LambdaQueryWrapper<RecipeEntity>().in(RecipeEntity::getDishId, dishIds))
                .stream()
                .collect(Collectors.toMap(RecipeEntity::getDishId, Function.identity(), (a, b) -> a));
        List<Long> creatorIds = entities.stream()
                .map(DishEntity::getCreatedBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, UserEntity> userById = creatorIds.isEmpty()
                ? Map.of()
                : userRepository.selectBatchIds(creatorIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity(), (a, b) -> a));
        List<DishTemplateVo> records = new ArrayList<>(entities.size());
        for (DishEntity entity : entities) {
            records.add(toVo(entity, recipeByDish.get(entity.getId()),
                    userById.get(entity.getCreatedBy())));
        }
        return records;
    }

    /**
     * 实体转展示对象；兼容历史 JSON（AI 草稿使用 durationMinutes/content）。
     *
     * @param dish    模板实体
     * @param recipe  菜谱实体，可空
     * @param creator 创建人，可空
     * @return 模板 VO
     * @history 1.00 2026-10-31 12:00 XieMingJie Created.
     */
    private DishTemplateVo toVo(DishEntity dish,
                                RecipeEntity recipe,
                                UserEntity creator) {
        String status = dish.getTemplateStatus();
        DishTemplateVo vo = new DishTemplateVo()
                .setId(dish.getId())
                .setKitchenId(dish.getKitchenId())
                .setOrgUnitId(dish.getOrgUnitId())
                .setName(dish.getName())
                .setSubtitle(dish.getSubtitle())
                .setCoverImage(dish.getCoverImage())
                .setDescription(dish.getDescription())
                .setTags(dish.getTags() == null ? List.of() : dish.getTags())
                .setStatus(status)
                .setSourceType(dish.getSourceType())
                .setSourceUrl(dish.getSourceUrl())
                .setEstimatedTotalSeconds(dish.getEstimatedTotalSeconds())
                .setCreatorId(dish.getCreatedBy())
                .setCreatorName(creator == null ? null : creator.getNickname())
                .setCreateTime(dish.getCreateTime())
                .setLastModifyTime(dish.getLastModifyTime());
        int usageCount = dish.getUsageCount() == null ? 0 : dish.getUsageCount();
        vo.setUsageCount(usageCount);
        vo.setMade(usageCount > 0);
        if (recipe != null) {
            vo.setServings(recipe.getServings())
                    .setDifficulty(recipe.getDifficulty());
            vo.setIngredients(toMaterials(recipe.getIngredients()));
            vo.setSteps(toSteps(recipe.getSteps()));
        } else {
            vo.setIngredients(List.of());
            vo.setSteps(List.of());
        }
        boolean archived = DishTemplateStatus.ARCHIVED.equals(status);
        vo.setEditable(!archived);
        vo.setPublishable((DishTemplateStatus.DRAFT.equals(status)
                || DishTemplateStatus.PRE_CREATED.equals(status))
                && vo.getSteps() != null && !vo.getSteps().isEmpty());
        vo.setArchivable(!archived);
        return vo;
    }

    private List<RecipeMaterialDto> toMaterials(List<Map<String, Object>> maps) {
        if (maps == null || maps.isEmpty()) {
            return List.of();
        }
        List<RecipeMaterialDto> list = new ArrayList<>(maps.size());
        for (Map<String, Object> map : maps) {
            if (map == null) {
                continue;
            }
            list.add(new RecipeMaterialDto()
                    .setName(asText(map.get("name")))
                    .setAmount(asText(map.get("amount")))
                    .setUnit(asText(map.get("unit")))
                    .setProcessing(asText(map.get("processing")))
                    .setRequired(asBoolean(map.get("required"), true))
                    .setSubstitute(asText(map.get("substitute")))
                    .setNote(asText(map.get("note"))));
        }
        return list;
    }

    private List<RecipeStepDto> toSteps(List<Map<String, Object>> maps) {
        if (maps == null || maps.isEmpty()) {
            return List.of();
        }
        List<RecipeStepDto> list = new ArrayList<>(maps.size());
        int index = 1;
        for (Map<String, Object> map : maps) {
            if (map == null) {
                index++;
                continue;
            }
            String description = asText(map.get("description"));
            if (description == null) {
                description = asText(map.get("content"));
            }
            Integer order = asInteger(map.get("order"));
            if (order == null) {
                order = asInteger(map.get("step"));
            }
            if (order == null) {
                order = index;
            }
            Integer duration = asInteger(map.get("durationSeconds"));
            if (duration == null) {
                Integer minutes = asInteger(map.get("durationMinutes"));
                duration = minutes == null ? null : minutes * 60;
            }
            String timerType = asText(map.get("timerType"));
            if (timerType == null) {
                timerType = duration != null && duration > 0 ? "countdown" : "none";
            }
            list.add(new RecipeStepDto()
                    .setStep(order)
                    .setOrder(order)
                    .setTitle(asText(map.get("title")))
                    .setDescription(description)
                    .setImage(asText(map.get("image")))
                    .setVideo(asText(map.get("video")))
                    .setDurationSeconds(duration)
                    .setTimerType(timerType.toLowerCase(Locale.ROOT))
                    .setParallelGroup(asText(map.get("parallelGroup")))
                    .setCompletionHint(asText(map.get("completionHint")))
                    .setWarning(asText(map.get("warning")))
                    .setRequired(asBoolean(map.get("required"), true))
                    .setCanUndo(asBoolean(map.get("canUndo"), true)));
            index++;
        }
        return list;
    }

    private static String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean asBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(text);
    }
}
