package cn.miyf.service;

import cn.miyf.bean.vo.CaptchaVo;
import cn.miyf.bean.vo.LoginRiskVo;
import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.config.RedisAppProperties;
import cn.miyf.infrastructure.redis.CacheKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 管理端登录风控：失败累计、验证码（≥3）、阶梯封禁（≥6）。
 * <p>
 * 阶梯：6→5min，7→10min，8→30min，9→2h，≥10→24h；成功登录清零。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Service
public class LoginProtectService {

    private static final Logger log = LoggerFactory.getLogger(LoginProtectService.class);

    /**
     * 验证码门槛
     */
    public static final int CAPTCHA_THRESHOLD = 3;
    /**
     * 封禁门槛
     */
    public static final int LOCK_THRESHOLD = 6;
    /**
     * 失败计数窗口（秒）
     */
    private static final long FAIL_WINDOW_SECONDS = 24 * 3600L;
    /**
     * 验证码有效期（秒）
     */
    private static final int CAPTCHA_TTL_SECONDS = 120;

    private static final long[] LOCK_SECONDS = {
            5 * 60L,
            10 * 60L,
            30 * 60L,
            2 * 3600L,
            24 * 3600L
    };

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisAppProperties properties;

    /**
     * 构造登录风控服务。
     *
     * @param stringRedisTemplate Redis
     * @param properties          配置
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public LoginProtectService(StringRedisTemplate stringRedisTemplate, RedisAppProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
    }

    /**
     * 查询登录风控状态。
     *
     * @param principalKey admin:username
     * @return 状态
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public LoginRiskVo status(String principalKey) {
        int failCount = getFailCount(principalKey);
        Long lockedUntil = getLockedUntilMillis(principalKey);
        long remain = 0;
        boolean locked = false;
        if (lockedUntil != null) {
            long now = Instant.now().toEpochMilli();
            if (lockedUntil > now) {
                locked = true;
                remain = (lockedUntil - now + 999) / 1000;
            }
        }
        return new LoginRiskVo()
                .setFailCount(failCount)
                .setCaptchaRequired(failCount >= CAPTCHA_THRESHOLD)
                .setLocked(locked)
                .setLockedUntil(locked ? lockedUntil : null)
                .setLockRemainSeconds(remain);
    }

    /**
     * 登录前校验：锁定、验证码。
     *
     * @param principalKey 主体
     * @param captchaId    验证码 ID
     * @param captchaCode  用户输入
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public void assertCanAttempt(String principalKey, String captchaId, String captchaCode) {
        LoginRiskVo risk = status(principalKey);
        if (risk.isLocked()) {
            throw new BusinessException(
                    ErrorCode.ACCOUNT_LOCKED,
                    "登录失败次数过多，请 " + formatRemain(risk.getLockRemainSeconds()) + " 后再试",
                    risk);
        }
        if (risk.isCaptchaRequired()) {
            if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(captchaCode)) {
                throw new BusinessException(ErrorCode.CAPTCHA_REQUIRED, "请输入验证码", risk);
            }
            verifyCaptcha(captchaId, captchaCode);
        }
    }

    /**
     * 记录一次登录失败，必要时升级锁定。
     *
     * @param principalKey 主体
     * @return 最新风控状态
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public LoginRiskVo recordFailure(String principalKey) {
        if (!properties.isEnabled() || !properties.getRateLimit().isEnabled()) {
            return status(principalKey);
        }
        try {
            String failKey = CacheKeys.loginFail(principalKey);
            Long count = stringRedisTemplate.opsForValue().increment(failKey);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(failKey, FAIL_WINDOW_SECONDS, TimeUnit.SECONDS);
            }
            int failCount = count == null ? 0 : count.intValue();
            if (failCount >= LOCK_THRESHOLD) {
                long lockSec = lockSecondsForFailCount(failCount);
                long until = Instant.now().getEpochSecond() + lockSec;
                stringRedisTemplate.opsForValue().set(
                        CacheKeys.loginLock(principalKey),
                        String.valueOf(until),
                        lockSec,
                        TimeUnit.SECONDS);
            }
        } catch (Exception ex) {
            log.warn("record login failure failed. principal={}", principalKey, ex);
        }
        return status(principalKey);
    }

    /**
     * 登录成功清除风控状态。
     *
     * @param principalKey 主体
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public void clear(String principalKey) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            stringRedisTemplate.delete(CacheKeys.loginFail(principalKey));
            stringRedisTemplate.delete(CacheKeys.loginLock(principalKey));
        } catch (Exception ex) {
            log.warn("clear login protect failed. principal={}", principalKey, ex);
        }
    }

    /**
     * 生成图形验证码。
     *
     * @return 验证码 VO
     * @history 1.00 2026-09-06 XieMingJie Created.
     */
    public CaptchaVo createCaptcha() {
        String code = randomCode(4);
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        try {
            stringRedisTemplate.opsForValue().set(
                    CacheKeys.captcha(captchaId),
                    code.toLowerCase(),
                    CAPTCHA_TTL_SECONDS,
                    TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.warn("store captcha failed", ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "验证码服务暂不可用");
        }
        String imageBase64 = renderPngBase64(code);
        return new CaptchaVo()
                .setCaptchaId(captchaId)
                .setImageBase64(imageBase64)
                .setExpireSeconds(CAPTCHA_TTL_SECONDS);
    }

    private void verifyCaptcha(String captchaId, String captchaCode) {
        String key = CacheKeys.captcha(captchaId);
        try {
            String expect = stringRedisTemplate.opsForValue().get(key);
            stringRedisTemplate.delete(key);
            if (expect == null || !expect.equalsIgnoreCase(captchaCode.trim())) {
                throw new BusinessException(ErrorCode.CAPTCHA_REQUIRED, "验证码错误或已过期，请刷新后重试");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("verify captcha failed", ex);
            throw new BusinessException(ErrorCode.CAPTCHA_REQUIRED, "验证码校验失败，请刷新后重试");
        }
    }

    private int getFailCount(String principalKey) {
        if (!properties.isEnabled()) {
            return 0;
        }
        try {
            String raw = stringRedisTemplate.opsForValue().get(CacheKeys.loginFail(principalKey));
            if (raw == null) {
                return 0;
            }
            return Integer.parseInt(raw);
        } catch (Exception ex) {
            log.warn("read fail count failed", ex);
            return 0;
        }
    }

    private Long getLockedUntilMillis(String principalKey) {
        if (!properties.isEnabled()) {
            return null;
        }
        try {
            String raw = stringRedisTemplate.opsForValue().get(CacheKeys.loginLock(principalKey));
            if (raw == null) {
                return null;
            }
            long epochSec = Long.parseLong(raw);
            return epochSec * 1000L;
        } catch (Exception ex) {
            log.warn("read lock failed", ex);
            return null;
        }
    }

    private static long lockSecondsForFailCount(int failCount) {
        int idx = failCount - LOCK_THRESHOLD;
        if (idx < 0) {
            idx = 0;
        }
        if (idx >= LOCK_SECONDS.length) {
            idx = LOCK_SECONDS.length - 1;
        }
        return LOCK_SECONDS[idx];
    }

    private static String formatRemain(long seconds) {
        if (seconds <= 60) {
            return seconds + " 秒";
        }
        if (seconds < 3600) {
            return ((seconds + 59) / 60) + " 分钟";
        }
        return ((seconds + 3599) / 3600) + " 小时";
    }

    private static String randomCode(int len) {
        // 去掉易混淆字符
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(len);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static String renderPngBase64(String code) {
        int w = 120;
        int h = 40;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(255, 253, 248));
            g.fillRect(0, 0, w, h);
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < 6; i++) {
                g.setColor(new Color(180 + r.nextInt(60), 180 + r.nextInt(60), 180 + r.nextInt(60)));
                g.drawLine(r.nextInt(w), r.nextInt(h), r.nextInt(w), r.nextInt(h));
            }
            g.setFont(new Font("Arial", Font.BOLD, 24));
            for (int i = 0; i < code.length(); i++) {
                g.setColor(new Color(40 + r.nextInt(80), 60 + r.nextInt(80), 40 + r.nextInt(60)));
                g.drawString(String.valueOf(code.charAt(i)), 16 + i * 24, 28 + r.nextInt(5));
            }
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "验证码生成失败");
        }
    }
}
