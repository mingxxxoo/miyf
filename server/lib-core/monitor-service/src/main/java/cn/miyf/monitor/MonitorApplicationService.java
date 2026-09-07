package cn.miyf.monitor;

import cn.miyf.infrastructure.datasource.DataSourceKey;
import cn.miyf.infrastructure.datasource.DataSourceRegistry;
import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 主机与组件监控采集。
 *
 * @author XieMingJie
 * @since 2026-09-06
 */
@Service
public class MonitorApplicationService {

    private static final int RECENT_ERROR_LIMIT = 50;

    private final DataSourceRegistry dataSourceRegistry;
    private final ObjectProvider<StringRedisTemplate> stringRedisTemplate;
    private final Deque<Map<String, Object>> recentErrors = new ArrayDeque<>();

    public MonitorApplicationService(DataSourceRegistry dataSourceRegistry,
                                     ObjectProvider<StringRedisTemplate> stringRedisTemplate) {
        this.dataSourceRegistry = dataSourceRegistry;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("collectedTime", System.currentTimeMillis());
        result.put("jvm", jvmSnapshot());
        result.put("disk", diskSnapshot());
        List<ComponentHealthSnapshot> components = List.of(
                redisSnapshot(),
                dbSnapshot(DataSourceKey.PRIMARY),
                dbSnapshot(DataSourceKey.SECONDARY));
        result.put("components", components);
        synchronized (recentErrors) {
            result.put("recentErrors", new ArrayList<>(recentErrors));
        }
        return result;
    }

    private Map<String, Object> jvmSnapshot() {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("uptimeMs", runtime.getUptime());
        map.put("heapUsed", memory.getHeapMemoryUsage().getUsed());
        map.put("heapMax", memory.getHeapMemoryUsage().getMax());
        map.put("nonHeapUsed", memory.getNonHeapMemoryUsage().getUsed());
        map.put("processors", os.getAvailableProcessors());
        map.put("systemLoadAverage", os.getSystemLoadAverage());
        map.put("threadCount", threads.getThreadCount());
        map.put("daemonThreadCount", threads.getDaemonThreadCount());
        map.put("peakThreadCount", threads.getPeakThreadCount());
        map.put("javaVersion", System.getProperty("java.version"));
        map.put("osName", System.getProperty("os.name"));
        map.put("osArch", System.getProperty("os.arch"));
        try {
            if (os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
                map.put("processCpuLoad", sunOs.getProcessCpuLoad());
                map.put("systemCpuLoad", sunOs.getCpuLoad());
                long committedVirtual = sunOs.getCommittedVirtualMemorySize();
                if (committedVirtual > 0) {
                    map.put("committedVirtualMemory", committedVirtual);
                }
            }
        } catch (Throwable ignored) {
            // 非 HotSpot / 受限环境忽略扩展 CPU 指标
        }
        return map;
    }

    private Map<String, Object> diskSnapshot() {
        File root = new File(".").getAbsoluteFile();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("path", root.getAbsolutePath());
        map.put("total", root.getTotalSpace());
        map.put("free", root.getFreeSpace());
        map.put("usable", root.getUsableSpace());
        return map;
    }

    private ComponentHealthSnapshot redisSnapshot() {
        ComponentHealthSnapshot snap = new ComponentHealthSnapshot().setName("redis");
        StringRedisTemplate redis = stringRedisTemplate.getIfAvailable();
        if (redis == null) {
            return snap.setStatus("DISABLED").put("enabled", false);
        }
        try {
            redis.hasKey("__miyf_monitor_ping__");
            snap.setStatus("UP").put("reachable", true).put("enabled", true);
        } catch (Exception ex) {
            snap.setStatus("DOWN").put("error", ex.getMessage()).put("enabled", true);
            recordError("redis", ex.getMessage());
        }
        return snap;
    }

    private ComponentHealthSnapshot dbSnapshot(DataSourceKey key) {
        String name = "db-" + key.name().toLowerCase();
        ComponentHealthSnapshot snap = new ComponentHealthSnapshot().setName(name);
        Optional<DataSource> optional = dataSourceRegistry.get(key);
        if (optional.isEmpty()) {
            return snap.setStatus("DISABLED").put("enabled", false);
        }
        DataSource dataSource = optional.get();
        putPoolDetails(snap, dataSource);
        try (Connection conn = dataSource.getConnection()) {
            boolean up = conn.isValid(2);
            snap.setStatus(up ? "UP" : "DOWN")
                    .put("enabled", true)
                    .put("catalog", conn.getCatalog());
            if (!up) {
                recordError(name, "connection isValid=false");
            }
        } catch (Exception ex) {
            snap.setStatus("DOWN").put("error", ex.getMessage()).put("enabled", true);
            recordError(name, ex.getMessage());
        }
        return snap;
    }

    private void putPoolDetails(ComponentHealthSnapshot snap, DataSource dataSource) {
        DruidDataSource druid = unwrapDruid(dataSource);
        if (druid == null) {
            return;
        }
        Map<String, Object> pool = new LinkedHashMap<>();
        int active = druid.getActiveCount();
        int pooling = druid.getPoolingCount();
        int maxActive = druid.getMaxActive();
        pool.put("active", active);
        pool.put("pooling", pooling);
        pool.put("maxActive", maxActive);
        pool.put("createCount", druid.getCreateCount());
        pool.put("destroyCount", druid.getDestroyCount());
        pool.put("connectCount", druid.getConnectCount());
        pool.put("closeCount", druid.getCloseCount());
        snap.put("pool", pool)
                .put("activeConnections", active)
                .put("idleConnections", pooling)
                .put("maxConnections", maxActive);
    }

    private static DruidDataSource unwrapDruid(DataSource dataSource) {
        if (dataSource instanceof DruidDataSource druid) {
            return druid;
        }
        try {
            return dataSource.unwrap(DruidDataSource.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void recordError(String component, String message) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("at", Instant.now().toString());
        entry.put("component", component);
        entry.put("message", message == null ? "" : message);
        synchronized (recentErrors) {
            recentErrors.addFirst(entry);
            while (recentErrors.size() > RECENT_ERROR_LIMIT) {
                recentErrors.removeLast();
            }
        }
    }
}
