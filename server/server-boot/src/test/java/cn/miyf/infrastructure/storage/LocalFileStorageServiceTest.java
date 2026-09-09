package cn.miyf.infrastructure.storage;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.config.FileStorageProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本地存储与上传校验单测。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:19
 */
class LocalFileStorageServiceTest {

    /**
     * Windows 下 JVM/杀毒可能短时锁住临时文件，JUnit 默认清理会失败；
     * 使用 NEVER + 测试后尽力删除，避免误导性错误。
     */
    @TempDir(cleanup = CleanupMode.NEVER)
    Path tempDir;

    private FileStorageProperties properties;
    private LocalFileStorageService storage;

    @BeforeEach
    void setUp() {
        properties = new FileStorageProperties();
        properties.setType("local");
        properties.setPath(tempDir.toString());
        properties.setBaseUrl("http://localhost:8080");
        properties.setPathNamespace("miyf");
        properties.setMaxSizeBytes(1024);
        storage = new LocalFileStorageService(properties);
    }

    @AfterEach
    void tearDown() {
        if (tempDir == null || !Files.exists(tempDir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(tempDir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Windows 偶发锁文件：忽略，避免测试失败
                }
            });
        } catch (IOException ignored) {
            // ignore
        }
    }

    @Test
    void store_shouldAcceptValidJpegWithoutExtension() throws Exception {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02};
        String path = StoragePathUtils.buildStoragePath("miyf", "kitchen", 12345L);
        StoredFile stored = storage.store(new ByteArrayInputStream(jpeg), jpeg.length, "image/jpeg", path);
        assertEquals("image/jpeg", stored.contentType());
        assertEquals(path, stored.path());
        assertFalse(stored.path().contains("."));
        assertTrue(Files.exists(storage.resolveSafe(stored.path())));
        assertEquals(32, stored.md5().length());
        assertEquals("http://localhost:8080/r/12345", StoragePathUtils.publicResourceUrl(properties.getBaseUrl(), 12345L));
        assertTrue(path.startsWith("miyf/kitchen/"));
        assertTrue(path.endsWith("/12345"));
    }

    @Test
    void store_shouldRejectOversized() {
        byte[] jpeg = new byte[2048];
        jpeg[0] = (byte) 0xFF;
        jpeg[1] = (byte) 0xD8;
        jpeg[2] = (byte) 0xFF;
        BusinessException ex = assertThrows(BusinessException.class,
                () -> storage.store(new ByteArrayInputStream(jpeg), jpeg.length, "image/jpeg", "miyf/kitchen/2026/09/1"));
        assertEquals(ErrorCode.FILE_TOO_LARGE.getCode(), ex.getCode());
    }

    @Test
    void store_shouldRejectInvalidMagic() {
        byte[] data = new byte[]{0x00, 0x01, 0x02, 0x03};
        BusinessException ex = assertThrows(BusinessException.class,
                () -> storage.store(new ByteArrayInputStream(data), data.length, "image/png", "miyf/health/2026/09/2"));
        assertEquals(ErrorCode.INVALID_FILE.getCode(), ex.getCode());
    }

    @Test
    void detectByMagic_shouldRecognizePngAndWebp() {
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertEquals("image/png", FileUploadValidator.detectByMagic(png));
        byte[] webp = new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
        assertEquals("image/webp", FileUploadValidator.detectByMagic(webp));
    }
}
