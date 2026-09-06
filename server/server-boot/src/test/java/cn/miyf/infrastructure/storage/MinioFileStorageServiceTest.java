package cn.miyf.infrastructure.storage;

import cn.miyf.common.BusinessException;
import cn.miyf.common.ErrorCode;
import cn.miyf.config.FileStorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MinIO 存储单测（Mock 客户端）。
 *
 * @author XieMingJie
 * @since 2026-09-05 09:22
 */
@ExtendWith(MockitoExtension.class)
class MinioFileStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    private FileStorageProperties properties;
    private MinioFileStorageService storage;

    @BeforeEach
    void setUp() throws Exception {
        properties = new FileStorageProperties();
        properties.setType("minio");
        properties.setEndpoint("http://localhost:9000");
        properties.setBucket("miyf");
        properties.setBaseUrl("http://localhost:9000/miyf");
        properties.setAccessKey("minioadmin");
        properties.setSecretKey("minioadmin");
        properties.setMaxSizeBytes(1024);
        properties.setAutoCreateBucket(true);
        properties.setPublicRead(false);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        storage = new MinioFileStorageService(properties, minioClient);
    }

    @Test
    void store_shouldPutValidJpeg() throws Exception {
        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01};
        StoredFile stored = storage.store(new ByteArrayInputStream(jpeg), jpeg.length, "image/jpeg", "a.jpg");
        assertEquals("image/jpeg", stored.contentType());
        assertTrue(stored.url().startsWith("http://localhost:9000/miyf/"));
        assertTrue(stored.objectKey().endsWith(".jpg"));
        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());
        assertEquals("miyf", captor.getValue().bucket());
        assertEquals(stored.objectKey(), captor.getValue().object());
    }

    @Test
    void store_shouldRejectInvalidMagicBeforePut() {
        byte[] data = new byte[]{0x00, 0x01, 0x02};
        BusinessException ex = assertThrows(BusinessException.class,
                () -> storage.store(new ByteArrayInputStream(data), data.length, "image/png", "x.png"));
        assertEquals(ErrorCode.INVALID_FILE.getCode(), ex.getCode());
    }
}
