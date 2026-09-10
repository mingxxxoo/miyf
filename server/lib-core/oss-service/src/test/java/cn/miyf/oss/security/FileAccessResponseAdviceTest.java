package cn.miyf.oss.security;

import cn.miyf.common.ApiResult;
import cn.miyf.config.FileStorageProperties;
import cn.miyf.infrastructure.cache.NoopCacheClient;
import cn.miyf.oss.bean.vo.UploadedFileVo;
import cn.miyf.service.SystemConfigReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link FileAccessResponseAdvice} 签名改写单测。
 *
 * @author XieMingJie
 * @since 2026-09-10
 */
class FileAccessResponseAdviceTest {

    private FileAccessResponseAdvice advice;
    private FileAccessSigner signer;

    @BeforeEach
    void setUp() {
        FileStorageProperties props = new FileStorageProperties();
        props.setAccessSignSecret("unit-test-file-access-sign-secret-32!");
        FileAccessPermissionCache cache = new FileAccessPermissionCache(new NoopCacheClient());
        SystemConfigReader reader = mock(SystemConfigReader.class);
        when(reader.getLong(eq(FileAccessSigner.CONFIG_SIGN_TTL_SECONDS), anyLong())).thenReturn(3600L);
        signer = new FileAccessSigner(props, cache, reader);
        advice = new FileAccessResponseAdvice(cache, signer);
    }

    @Test
    void beforeBodyWrite_shouldRewriteAnnotatedUrlFields() throws Exception {
        UploadedFileVo vo = new UploadedFileVo()
                .setId(100L)
                .setUrl("http://localhost:8080/r/100");
        ApiResult<UploadedFileVo> body = ApiResult.ok(vo);

        MethodParameter returnType = mock(MethodParameter.class);
        Object written = advice.beforeBodyWrite(
                body,
                returnType,
                MediaType.APPLICATION_JSON,
                JacksonJsonHttpMessageConverter.class,
                new ServletServerHttpRequest(new MockHttpServletRequest()),
                new ServletServerHttpResponse(new MockHttpServletResponse()));

        @SuppressWarnings("unchecked")
        ApiResult<UploadedFileVo> result = (ApiResult<UploadedFileVo>) written;
        String url = result.data().getUrl();
        assertNotNull(url);
        assertTrue(url.contains("exp="));
        assertTrue(url.contains("sig="));
        assertTrue(signer.verify(100L, Long.parseLong(query(url, "exp")), query(url, "sig")));
    }

    @Test
    void beforeBodyWrite_shouldRewriteListOfUrls() throws Exception {
        DishImagesFixture fixture = new DishImagesFixture();
        fixture.images = List.of("http://localhost:8080/r/1", "http://localhost:8080/r/2");

        advice.beforeBodyWrite(
                ApiResult.ok(fixture),
                mock(MethodParameter.class),
                MediaType.APPLICATION_JSON,
                JacksonJsonHttpMessageConverter.class,
                new ServletServerHttpRequest(new MockHttpServletRequest()),
                new ServletServerHttpResponse(new MockHttpServletResponse()));

        assertTrue(fixture.images.get(0).contains("sig="));
        assertTrue(fixture.images.get(1).contains("sig="));
    }

    private static String query(String url, String key) {
        for (String part : url.substring(url.indexOf('?') + 1).split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0 && key.equals(part.substring(0, eq))) {
                return part.substring(eq + 1);
            }
        }
        throw new IllegalArgumentException(key);
    }

    /** 仅用于反射扫描的测试夹具。 */
    public static class DishImagesFixture {
        @FileAccess
        public List<String> images;
    }
}
