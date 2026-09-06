package cn.miyf.infrastructure.storage;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.config.FileStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本地存储与上传校验单测。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:19
 */
class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageProperties properties;
    private LocalFileStorageService storage;

    @BeforeEach
    void setUp() {
        properties = new FileStorageProperties();
        properties.setType("local");
        properties.setPath(tempDir.toString());
        properties.setBaseUrl("http://localhost:8080/uploads");
        properties.setMaxSizeBytes(1024);
        storage = new LocalFileStorageService(properties);
    }

    @Test
    void store_shouldAcceptValidJpeg() throws Exception {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01, 0x02};
        StoredFile stored = storage.store(new ByteArrayInputStream(jpeg), jpeg.length, "image/jpeg", "x.jpg");
        assertEquals("image/jpeg", stored.contentType());
        assertTrue(stored.url().startsWith("http://localhost:8080/uploads/"));
        assertTrue(stored.objectKey().endsWith(".jpg"));
        assertTrue(Files.exists(storage.resolveSafe(stored.objectKey())));
    }

    @Test
    void store_shouldRejectOversized() {
        byte[] jpeg = new byte[2048];
        jpeg[0] = (byte) 0xFF;
        jpeg[1] = (byte) 0xD8;
        jpeg[2] = (byte) 0xFF;
        BusinessException ex = assertThrows(BusinessException.class,
                () -> storage.store(new ByteArrayInputStream(jpeg), jpeg.length, "image/jpeg", "big.jpg"));
        assertEquals(ErrorCode.FILE_TOO_LARGE.getCode(), ex.getCode());
    }

    @Test
    void store_shouldRejectInvalidMagic() {
        byte[] data = new byte[]{0x00, 0x01, 0x02, 0x03};
        BusinessException ex = assertThrows(BusinessException.class,
                () -> storage.store(new ByteArrayInputStream(data), data.length, "image/png", "fake.png"));
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
