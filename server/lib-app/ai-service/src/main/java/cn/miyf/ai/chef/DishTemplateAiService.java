package cn.miyf.ai.chef;

import cn.miyf.ai.bean.dto.DishTemplateAiTextDto;
import cn.miyf.ai.bean.model.DishTemplateExtractResult;
import cn.miyf.ai.bean.vo.DishTemplateAiExtractVo;
import cn.miyf.ai.config.AiProperties;
import cn.miyf.ai.prompt.AiPromptTemplates;
import cn.miyf.ai.support.AiStructuredCaller;
import cn.miyf.ai.support.DishTemplateExtractNormalizer;
import cn.miyf.ai.support.WebPageFetcher;
import cn.miyf.auth.security.SecurityUtils;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.kitchen.bean.entity.KitchenEntity;
import cn.miyf.kitchen.service.KitchenAccessService;
import cn.miyf.oss.bean.dto.FileUploadCommand;
import cn.miyf.oss.bean.vo.UploadedFileVo;
import cn.miyf.oss.enums.FileAccessPermission;
import cn.miyf.oss.service.FileResourceApplicationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 场景四：文本 / 网页链接 / 菜品照片 → 组织菜品库草稿。
 * <p>
 * 复用现有 OpenCode 视觉模型完成多模态识别：图片先走 OSS 压缩存储（最长边 1280、JPEG），
 * 再以 base64 字节提交模型，避免模型侧拉取内网资源。识别结果只生成草稿：
 * 不落库、不自动发布，成员需在确认页核对后走 {@code /dishes/templates} 保存。
 *
 * @author XieMingJie
 * @since 2026-10-31 18:00
 * @history 1.00 2026-10-31 18:00 XieMingJie Created.
 */
@Service
@RequiredArgsConstructor
public class DishTemplateAiService {

    private static final Logger log = LoggerFactory.getLogger(DishTemplateAiService.class);

    private static final String SCENE = "DISH_TEMPLATE";
    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final int MAX_TEXT_LENGTH = 4000;
    private static final int MAX_AI_IMAGE_BYTES = 6 * 1024 * 1024;

    private final AiStructuredCaller aiStructuredCaller;
    private final AiProperties aiProperties;
    private final WebPageFetcher webPageFetcher;
    private final KitchenAccessService kitchenAccessService;
    private final FileResourceApplicationService fileResourceApplicationService;

    /**
     * 文本或网页链接抽取菜品草稿。
     *
     * @param dto 请求，文本与链接二选一，优先文本
     * @return 确认页草稿
     * @history 1.00 2026-10-31 18:00 XieMingJie Created.
     */
    public DishTemplateAiExtractVo extractText(DishTemplateAiTextDto dto) {
        KitchenEntity kitchen = requireKitchen();
        String text = dto == null ? null : clean(dto.getText());
        if (text != null) {
            if (text.length() > MAX_TEXT_LENGTH) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "文字素材不能超过 " + MAX_TEXT_LENGTH + " 字");
            }
            return callAi(kitchen, "AI_TEXT", null, null,
                    AiPromptTemplates.dishTemplateTextUser(text), summarize(text), List.of());
        }
        String url = dto == null ? null : clean(dto.getUrl());
        if (url == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请提供菜品文字或网页链接");
        }
        // 抓取失败直接抛业务异常，不进入 AI
        WebPageFetcher.FetchedPage page = webPageFetcher.fetch(url);
        String userPrompt = AiPromptTemplates.dishTemplateUrlUser(page.title(), page.finalUrl(), page.text());
        return callAi(kitchen, "AI_URL", page.finalUrl(), null,
                userPrompt, "url:" + page.finalUrl(), List.of());
    }

    /**
     * 菜品照片抽取菜品草稿。
     * <p>
     * 图片会先持久化并压缩为公开可读资源：识别失败时仍返回该地址，
     * 用户可以把它作为封面直接手动补录。
     *
     * @param file 菜品照片
     * @param text 可选补充说明
     * @return 确认页草稿
     * @history 1.00 2026-10-31 18:00 XieMingJie Created.
     */
    public DishTemplateAiExtractVo extractImage(MultipartFile file, String text) {
        KitchenEntity kitchen = requireKitchen();
        String contentType = requireImageType(file);
        UploadedFileVo stored = storeImage(file);
        byte[] bytes = readStoredImage(stored);
        if (bytes.length > MAX_AI_IMAGE_BYTES) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "图片过大，请压缩后重试");
        }
        // 存储层会按魔数纠正类型（压缩后通常为 image/jpeg），以实际存储类型提交模型
        String mediaType = StringUtils.hasText(stored.getContentType())
                ? stored.getContentType().trim().toLowerCase(Locale.ROOT)
                : contentType;
        Media media = Media.builder()
                .mimeType(MimeTypeUtils.parseMimeType(mediaType))
                .data(bytes)
                .name(String.valueOf(stored.getId()))
                .build();
        String userPrompt = AiPromptTemplates.dishTemplateImageUser(text);
        String summary = "image:" + (StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename().trim() : String.valueOf(stored.getId()));
        return callAi(kitchen, "AI_IMAGE", null, stored.getUrl(), userPrompt, summary, List.of(media));
    }

    /**
     * 统一调用模型并降级：任何异常都返回可手动补录的空草稿，不向前端抛 500。
     */
    private DishTemplateAiExtractVo callAi(KitchenEntity kitchen,
                                           String sourceType,
                                           String sourceUrl,
                                           String coverImage,
                                           String userPrompt,
                                           String inputSummary,
                                           List<Media> media) {
        if (!aiStructuredCaller.isAvailable()) {
            return DishTemplateExtractNormalizer.blank(
                    "AI 暂不可用，请手动填写菜品", true, sourceType, sourceUrl, coverImage);
        }
        try {
            DishTemplateExtractResult result = aiStructuredCaller.callEntity(
                    SCENE,
                    AiPromptTemplates.DISH_TEMPLATE_SYSTEM,
                    userPrompt,
                    aiProperties.getDishTemplate().getTemperature(),
                    aiProperties.getDishTemplate().getRetryOnTransient(),
                    DishTemplateExtractResult.class,
                    SecurityUtils.currentUserId(),
                    kitchen.getId(),
                    inputSummary,
                    media);
            return DishTemplateExtractNormalizer.normalize(result, sourceType, sourceUrl, coverImage);
        } catch (Exception ex) {
            log.warn("菜品模板 AI 识别失败 sourceType={} reason={}", sourceType, ex.toString());
            return DishTemplateExtractNormalizer.blank(
                    "AI 识别失败，请手动填写菜品", true, sourceType, sourceUrl, coverImage);
        }
    }

    /**
     * 当前成员厨房；同时要求厨房归属组织，避免识别完才发现无法保存。
     */
    private KitchenEntity requireKitchen() {
        KitchenEntity kitchen = kitchenAccessService.requireMemberKitchen();
        kitchenAccessService.requireOrgUnitId(kitchen);
        return kitchen;
    }

    /**
     * 校验并返回规范化后的图片 Content-Type。
     */
    private String requireImageType(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "请选择菜品图片");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "仅支持 JPG/PNG/WEBP 图片");
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        if (!IMAGE_TYPES.contains(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "仅支持 JPG/PNG/WEBP 图片");
        }
        return normalized;
    }

    /**
     * 存储图片：公开可读，供同组织成员查看封面；压缩后体积更小，适合直接提交视觉模型。
     */
    private UploadedFileVo storeImage(MultipartFile file) {
        FileUploadCommand command = new FileUploadCommand()
                .setAppCode("kitchen")
                .setSource("dish-template-ai")
                .setTemp(false)
                .setCompress(true)
                .setAccessPermission(FileAccessPermission.PUBLIC);
        try (InputStream in = file.getInputStream()) {
            return fileResourceApplicationService.store(
                    in,
                    file.getSize(),
                    file.getContentType(),
                    file.getOriginalFilename(),
                    command);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE, "读取图片失败");
        }
    }

    /**
     * 读取压缩后的存储内容；{@link Media} 的 OpenAI 适配只接受 byte[] 或 URL 字符串。
     */
    private byte[] readStoredImage(UploadedFileVo stored) {
        try (InputStream in = fileResourceApplicationService.open(stored.getId())) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE, "读取图片失败");
        }
    }

    private String summarize(String text) {
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 120 ? oneLine.substring(0, 120) : oneLine;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
