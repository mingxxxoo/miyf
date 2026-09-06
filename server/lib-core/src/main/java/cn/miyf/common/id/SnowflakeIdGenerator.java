package cn.miyf.common.id;

/**
 * 雪花算法 ID 生成器（64-bit）。
 * <p>
 * 结构：1 bit 符号位 + 41 bit 时间戳 + 5 bit 数据中心 + 5 bit 机器 + 12 bit 序列。
 *
 * @author XieMingJie
 * @since 2026-09-05 10:36
 */
public final class SnowflakeIdGenerator {

    private static final long EPOCH = 1704067200000L; // 2024-01-01 UTC+8 近似
    private static final long WORKER_BITS = 5L;
    private static final long DATACENTER_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER = ~(-1L << WORKER_BITS);
    private static final long MAX_DATACENTER = ~(-1L << DATACENTER_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long WORKER_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_SHIFT = SEQUENCE_BITS + WORKER_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_BITS + DATACENTER_BITS;

    private final long workerId;
    private final long datacenterId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * 构造生成器。
     *
     * @param workerId     机器 ID 0~31
     * @param datacenterId 数据中心 ID 0~31
     * @history 1.00 2026-09-05 10:36 XieMingJie Created.
     */
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER || workerId < 0) {
            throw new IllegalArgumentException("workerId out of range: " + workerId);
        }
        if (datacenterId > MAX_DATACENTER || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId out of range: " + datacenterId);
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 生成下一个 ID。
     *
     * @return 雪花 ID
     * @history 1.00 2026-09-05 10:36 XieMingJie Created.
     */
    public synchronized long nextId() {
        long timestamp = currentTime();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards: " + (lastTimestamp - timestamp) + "ms");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0L) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_SHIFT)
                | (workerId << WORKER_SHIFT)
                | sequence;
    }

    /**
     * 生成字符串形式的 ID（便于前端传递，避免 JS 精度问题）。
     *
     * @return 数字字符串
     * @history 1.00 2026-09-05 10:36 XieMingJie Created.
     */
    public String nextIdStr() {
        return Long.toString(nextId());
    }

    private long waitNextMillis(long last) {
        long ts = currentTime();
        while (ts <= last) {
            ts = currentTime();
        }
        return ts;
    }

    private long currentTime() {
        return System.currentTimeMillis();
    }
}
