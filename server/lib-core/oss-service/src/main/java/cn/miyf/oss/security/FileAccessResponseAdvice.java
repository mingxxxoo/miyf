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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 扫描响应体中标注 {@link FileAccess} 的字段。
 * 将可解析的文件引用改写为短期签名 URL（供 img / 小程序 Image 直开），
 * 并在存在登录主体时写入 Redis 临时访问权（兼容带 JWT 的下载）。
 *
 * @author XieMingJie
 * @since 2026-09-09
 * @history 1.00 2026-09-09 XieMingJie Created.
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class FileAccessResponseAdvice implements ResponseBodyAdvice<Object> {

    private final FileAccessPermissionCache fileAccessPermissionCache;
    private final FileAccessSigner fileAccessSigner;

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
     * 写出前扫描 {@link FileAccess} 字段：改写签名 URL，并批量授予 Redis 临时访问权。
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
        walk(body, fileIds, new IdentityHashMap<>());
        if (!fileIds.isEmpty()) {
            fileAccessPermissionCache.grantAll(fileIds);
        }
        return body;
    }

    private void walk(Object node, Set<Long> out, IdentityHashMap<Object, Boolean> visited) {
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
            walk(apiResult.data(), out, visited);
            return;
        }
        if (node instanceof PageResult<?> pageResult) {
            walk(pageResult.records(), out, visited);
            return;
        }
        if (node instanceof Collection<?> collection) {
            for (Object item : collection) {
                collectFileValue(item, out);
                walk(item, out, visited);
            }
            return;
        }
        if (node instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                collectFileValue(value, out);
                walk(value, out, visited);
            }
            return;
        }
        if (type.isArray()) {
            int len = Array.getLength(node);
            for (int i = 0; i < len; i++) {
                Object item = Array.get(node, i);
                collectFileValue(item, out);
                walk(item, out, visited);
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
                Object rewritten = rewriteAnnotatedValue(value, out, visited);
                if (rewritten != value) {
                    try {
                        field.set(node, rewritten);
                    } catch (IllegalAccessException ignored) {
                        // 不可写则仅依赖 Redis grant
                    }
                }
            } else {
                walk(value, out, visited);
            }
        }
    }

    private Object rewriteAnnotatedValue(Object value, Set<Long> out, IdentityHashMap<Object, Boolean> visited) {
        if (value instanceof List<?> list) {
            List<Object> next = new ArrayList<>(list.size());
            boolean changed = false;
            for (Object item : list) {
                Object rewritten = rewriteOne(item, out);
                changed |= rewritten != item;
                next.add(rewritten);
            }
            return changed ? next : value;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                collectFileValue(item, out);
            }
            return value;
        }
        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            boolean changed = false;
            for (int i = 0; i < len; i++) {
                Object item = Array.get(value, i);
                Object rewritten = rewriteOne(item, out);
                if (rewritten != item) {
                    Array.set(value, i, rewritten);
                    changed = true;
                }
            }
            return value;
        }
        Object rewritten = rewriteOne(value, out);
        if (rewritten == value) {
            walk(value, out, visited);
        }
        return rewritten;
    }

    private Object rewriteOne(Object value, Set<Long> out) {
        collectFileValue(value, out);
        Object signed = fileAccessSigner.signValue(value);
        if (signed instanceof String text) {
            collectFileValue(text, out);
        }
        return signed;
    }

    private void collectFileValue(Object value, Set<Long> out) {
        fileAccessPermissionCache.parseFileId(value).ifPresent(out::add);
    }

    private static boolean isApplicationType(Class<?> type) {
        String name = type.getName();
        return name.startsWith("cn.miyf.");
    }
}
