package cn.miyf.health.provider.huawei;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 华为 Token Redis 载荷加解密（AES-GCM）。
 * 明文以 {@code enc:v1:} 前缀区分旧数据；密钥由配置 secret 派生。
 *
 * @author XieMingJie
 * @since 2026-09-11
 */
public final class HuaweiTokenCrypto {

    private static final String PREFIX = "enc:v1:";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private HuaweiTokenCrypto() {
    }

    /**
     * 加密 JSON；secret 为空则原样返回（兼容未配置）。
     *
     * @param plain  明文 JSON
     * @param secret 派生密钥材料
     * @return 密文或明文
     */
    public static String seal(String plain, String secret) {
        if (!StringUtils.hasText(plain) || !StringUtils.hasText(secret)) {
            return plain;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(secret), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + cipherBytes.length);
            buf.put(iv);
            buf.put(cipherBytes);
            return PREFIX + Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "加密华为 token 失败");
        }
    }

    /**
     * 解密；无前缀则视为历史明文。
     *
     * @param stored 存库值
     * @param secret 派生密钥材料
     * @return 明文 JSON
     */
    public static String open(String stored, String secret) {
        if (!StringUtils.hasText(stored) || !stored.startsWith(PREFIX)) {
            return stored;
        }
        if (!StringUtils.hasText(secret)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "华为 token 已加密但未配置解密密钥");
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherBytes = new byte[all.length - GCM_IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(all, GCM_IV_LENGTH, cipherBytes, 0, cipherBytes.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(secret), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "解密华为 token 失败");
        }
    }

    private static SecretKey key(String secret) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(hash, "AES");
    }
}
