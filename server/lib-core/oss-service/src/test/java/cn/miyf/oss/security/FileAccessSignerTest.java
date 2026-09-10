package cn.miyf.oss.security;

import cn.miyf.config.FileStorageProperties;
import cn.miyf.infrastructure.cache.NoopCacheClient;
import cn.miyf.service.SystemConfigReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 文件访问签名单测。
 *
 * @author XieMingJie
 * @since 2026-09-10
 */
class FileAccessSignerTest {

    private FileAccessSigner signer;
    private SystemConfigReader systemConfigReader;

    @BeforeEach
    void setUp() {
        FileStorageProperties props = new FileStorageProperties();
        props.setAccessSignSecret("unit-test-file-access-sign-secret-32!");
        FileAccessPermissionCache cache = new FileAccessPermissionCache(new NoopCacheClient());
        systemConfigReader = mock(SystemConfigReader.class);
        when(systemConfigReader.getLong(eq(FileAccessSigner.CONFIG_SIGN_TTL_SECONDS), anyLong()))
                .thenReturn(3600L);
        signer = new FileAccessSigner(props, cache, systemConfigReader);
    }

    @Test
    void verify_shouldAcceptValidSignature() {
        long fileId = 1938L;
        long exp = Instant.now().getEpochSecond() + 600;
        String sig = signer.sign(fileId, exp);
        assertTrue(signer.verify(fileId, exp, sig));
        assertTrue(signer.verify(fileId, exp, sig.toUpperCase()));
    }

    @Test
    void verify_shouldRejectExpiredOrTampered() {
        long fileId = 1938L;
        long exp = Instant.now().getEpochSecond() - 10;
        String sig = signer.sign(fileId, exp);
        assertFalse(signer.verify(fileId, exp, sig));

        long future = Instant.now().getEpochSecond() + 600;
        String good = signer.sign(fileId, future);
        assertFalse(signer.verify(fileId, future, good + "00"));
        assertFalse(signer.verify(fileId + 1, future, good));
        assertFalse(signer.verify(fileId, null, good));
        assertFalse(signer.verify(fileId, future, null));
    }

    @Test
    void signUrl_shouldAppendExpAndSig() {
        String signed = signer.signUrl("http://localhost:8080/r/42");
        assertTrue(signed.startsWith("http://localhost:8080/r/42?"));
        assertTrue(signed.contains("exp="));
        assertTrue(signed.contains("sig="));
        assertTrue(signer.verify(42L, extractExp(signed), extractSig(signed)));
    }

    @Test
    void signUrl_shouldSkipWhenAlreadyValid() {
        String first = signer.signUrl("/r/7");
        String second = signer.signUrl(first);
        assertEquals(first, second);
    }

    @Test
    void signUrl_shouldResignWhenExpiredQueryPresent() {
        long past = Instant.now().getEpochSecond() - 30;
        String staleSig = signer.sign(9L, past);
        String stale = "http://localhost:8080/r/9?exp=" + past + "&sig=" + staleSig;
        String renewed = signer.signUrl(stale);
        assertNotEquals(stale, renewed);
        assertTrue(signer.verify(9L, extractExp(renewed), extractSig(renewed)));
    }

    @Test
    void signUrl_shouldHandlePlainId() {
        String signed = signer.signUrl("55");
        assertTrue(signed.startsWith("/r/55?"));
        assertTrue(signer.verify(55L, extractExp(signed), extractSig(signed)));
    }

    @Test
    void resolveTtlSeconds_shouldUseSystemConfig() {
        when(systemConfigReader.getLong(eq(FileAccessSigner.CONFIG_SIGN_TTL_SECONDS), anyLong()))
                .thenReturn(900L);
        assertEquals(900L, signer.resolveTtlSeconds());

        when(systemConfigReader.getLong(eq(FileAccessSigner.CONFIG_SIGN_TTL_SECONDS), anyLong()))
                .thenReturn(0L);
        assertEquals(FileAccessSigner.FALLBACK_TTL_SECONDS, signer.resolveTtlSeconds());
    }

    private static Long extractExp(String url) {
        return Long.parseLong(queryParam(url, "exp"));
    }

    private static String extractSig(String url) {
        return queryParam(url, "sig");
    }

    private static String queryParam(String url, String key) {
        String query = url.substring(url.indexOf('?') + 1);
        for (String part : query.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0 && key.equals(part.substring(0, eq))) {
                return part.substring(eq + 1);
            }
        }
        throw new IllegalArgumentException("missing " + key);
    }
}
