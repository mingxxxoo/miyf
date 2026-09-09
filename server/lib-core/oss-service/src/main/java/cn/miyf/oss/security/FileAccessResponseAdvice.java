package cn.miyf.oss.security;

import cn.miyf.common.ApiResult;
import cn.miyf.common.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 扫描响应体中标注 {@link FileAccess} 的字段，将文件 ID 写入 Redis 临时访问权。
 *
 * @author XieMingJie
 * @since 2026-09-09
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class FileAccessResponseAdvice implements ResponseBodyAdvice<Object> {

    private final FileAccessPermissionCache fileAccessPermissionCache;

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @history 1.00 2026-09-09 XieMingJie Created.
     */
    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (body == null) {
            return null;
        }
        Set<Long> fileIds = new HashSet<>();
        collect(body, fileIds, new IdentityHashMap<>());
        if (!fileIds.isEmpty()) {
            fileAccessPermissionCache.grantAll(fileIds);
        }
        return body;
    }

    private void collect(Object node, Set<Long> out, IdentityHashMap<Object, Boolean> visited) {
        if (node == null || visited.containsKey(node)) {
            return;
        }
        Class<?> type = node.getClass();
        if (type.isPrimitive()
                || type.isEnum()
                || CharSequence.class.isAssignableFrom(type)
                || Number.class.isAssignableFrom(type)
                || Boolean.class.isAssignableFrom(type)
                || type.getName().startsWith("java.time.")) {
            return;
        }
        visited.put(node, Boolean.TRUE);

        if (node instanceof ApiResult<?> apiResult) {
            collect(apiResult.data(), out, visited);
            return;
        }
        if (node instanceof PageResult<?> pageResult) {
            collect(pageResult.records(), out, visited);
            return;
        }
        if (node instanceof Collection<?> collection) {
            for (Object item : collection) {
                collectFileValue(item, out);
                collect(item, out, visited);
            }
            return;
        }
        if (node instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                collectFileValue(value, out);
                collect(value, out, visited);
            }
            return;
        }
        if (type.isArray()) {
            int len = Array.getLength(node);
            for (int i = 0; i < len; i++) {
                Object item = Array.get(node, i);
                collectFileValue(item, out);
                collect(item, out, visited);
            }
            return;
        }
        if (!isApplicationType(type)) {
            return;
        }
        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            Object value;
            try {
                value = field.get(node);
            } catch (IllegalAccessException ex) {
                continue;
            }
            if (value == null) {
                continue;
            }
            if (field.isAnnotationPresent(FileAccess.class)) {
                collectAnnotatedValue(value, out, visited);
            } else {
                collect(value, out, visited);
            }
        }
    }

    private void collectAnnotatedValue(Object value, Set<Long> out, IdentityHashMap<Object, Boolean> visited) {
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                collectFileValue(item, out);
            }
            return;
        }
        if (value != null && value.getClass().isArray()) {
            int len = Array.getLength(value);
            for (int i = 0; i < len; i++) {
                collectFileValue(Array.get(value, i), out);
            }
            return;
        }
        collectFileValue(value, out);
        // 若注解在嵌套对象上，继续下钻
        collect(value, out, visited);
    }

    private void collectFileValue(Object value, Set<Long> out) {
        fileAccessPermissionCache.parseFileId(value).ifPresent(out::add);
    }

    private static boolean isApplicationType(Class<?> type) {
        String name = type.getName();
        return name.startsWith("cn.miyf.");
    }
}
