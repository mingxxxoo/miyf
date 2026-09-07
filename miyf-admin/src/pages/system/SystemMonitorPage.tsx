import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Col, Row, Table, Typography } from 'antd';
import { notifyError } from '@/api/errors';
import type { StatusMeta } from '@/constants/status';
import { sysMonitorApi, type MonitorComponent, type MonitorOverview } from '@/modules/system/api';
import {
  EmptyState,
  MetricCard,
  PageHeader,
  SettingSection,
  StatusBadge,
} from '@/ui';

const COMP_STATUS: Record<string, StatusMeta> = {
  UP: { color: 'success', text: '正常' },
  DOWN: { color: 'error', text: '异常' },
  DISABLED: { color: 'default', text: '未启用' },
  UNKNOWN: { color: 'processing', text: '未知' },
};

function formatBytes(n?: number) {
  if (n == null || Number.isNaN(n) || n < 0) return '—';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let v = n;
  let i = 0;
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024;
    i += 1;
  }
  return `${v.toFixed(i === 0 ? 0 : 1)} ${units[i]}`;
}

function formatPct(ratio?: number) {
  if (ratio == null || ratio < 0) return '—';
  return `${(ratio * 100).toFixed(1)}%`;
}

function formatUptime(ms?: number) {
  if (ms == null || ms < 0) return '—';
  const s = Math.floor(ms / 1000);
  const d = Math.floor(s / 86400);
  const h = Math.floor((s % 86400) / 3600);
  const m = Math.floor((s % 3600) / 60);
  if (d > 0) return `${d}天 ${h}时`;
  if (h > 0) return `${h}时 ${m}分`;
  return `${m}分`;
}

/**
 * 系统监控：指标卡 + 组件表 + 连接池/近期错误（来自 details）。
 */
export default function SystemMonitorPage() {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<MonitorOverview | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      setData(await sysMonitorApi.overview());
    } catch (err) {
      setData(null);
      notifyError(err, '加载失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchData();
    const timer = window.setInterval(() => void fetchData(), 15000);
    return () => window.clearInterval(timer);
  }, [fetchData]);

  const jvm = data?.jvm;
  const disk = data?.disk;

  const cpuValue = useMemo(() => {
    if (jvm?.processCpuLoad != null && jvm.processCpuLoad >= 0) {
      return formatPct(jvm.processCpuLoad);
    }
    if (jvm?.systemCpuLoad != null && jvm.systemCpuLoad >= 0) {
      return formatPct(jvm.systemCpuLoad);
    }
    return '—';
  }, [jvm?.processCpuLoad, jvm?.systemCpuLoad]);

  const memValue = useMemo(() => {
    if (jvm?.heapUsed == null) return '—';
    return `${formatBytes(jvm.heapUsed)} / ${formatBytes(jvm.heapMax)}`;
  }, [jvm?.heapUsed, jvm?.heapMax]);

  const diskValue = useMemo(() => {
    if (!disk?.total) return '—';
    const used = disk.total - (disk.free ?? 0);
    return `${Math.round((used / disk.total) * 100)}%`;
  }, [disk]);

  const poolDetails = useMemo(() => {
    for (const c of data?.components ?? []) {
      const d = c.details;
      if (!d) continue;
      if (
        d.pool != null ||
        d.connectionPool != null ||
        d.activeConnections != null ||
        d.maxConnections != null ||
        d.idleConnections != null
      ) {
        return d;
      }
    }
    return null;
  }, [data?.components]);

  const recentErrors = useMemo(() => {
    if (data?.recentErrors != null) {
      const top = data.recentErrors;
      if (Array.isArray(top) && top.length === 0) return null;
      return top;
    }
    for (const c of data?.components ?? []) {
      const d = c.details;
      if (!d) continue;
      if (d.recentErrors != null || d.errors != null || d.lastError != null) {
        return d.recentErrors ?? d.errors ?? d.lastError ?? d;
      }
    }
    return null;
  }, [data?.components, data?.recentErrors]);

  return (
    <div className="ck-page">
      <PageHeader
        title="系统监控"
        description="CPU / 内存 / 磁盘 / JVM 与依赖组件状态"
        extra={
          <Button loading={loading} onClick={() => void fetchData()}>
            刷新
          </Button>
        }
      />

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={12} md={6}>
          <MetricCard
            label="CPU"
            value={cpuValue}
            hint={
              jvm?.systemCpuLoad != null && jvm.systemCpuLoad >= 0
                ? `系统 ${formatPct(jvm.systemCpuLoad)}`
                : jvm?.processors
                  ? `${jvm.processors} 核`
                  : undefined
            }
          />
        </Col>
        <Col xs={12} md={6}>
          <MetricCard label="内存 (Heap)" value={memValue} hint={`非堆 ${formatBytes(jvm?.nonHeapUsed)}`} />
        </Col>
        <Col xs={12} md={6}>
          <MetricCard
            label="磁盘"
            value={diskValue}
            hint={
              disk?.total
                ? `可用 ${formatBytes(disk.usable ?? disk.free)} / ${formatBytes(disk.total)}`
                : undefined
            }
          />
        </Col>
        <Col xs={12} md={6}>
          <MetricCard
            label="JVM"
            value={formatUptime(jvm?.uptimeMs)}
            hint={
              jvm
                ? `线程 ${jvm.threadCount ?? '—'} · Java ${jvm.javaVersion ?? '—'}`
                : data?.collectedTime
                  ? new Date(data.collectedTime).toLocaleString()
                  : '—'
            }
          />
        </Col>
      </Row>

      <SettingSection title="组件状态" description="API / 数据库 / Redis 等依赖探测结果">
        <Table<MonitorComponent>
          rowKey="name"
          loading={loading && !data}
          pagination={false}
          dataSource={data?.components ?? []}
          locale={{ emptyText: <EmptyState description="暂无组件数据" /> }}
          columns={[
            { title: '组件', dataIndex: 'name', width: 160 },
            {
              title: '状态',
              dataIndex: 'status',
              width: 120,
              render: (v: string) => <StatusBadge code={v || 'UNKNOWN'} map={COMP_STATUS} />,
            },
            {
              title: '详情',
              dataIndex: 'details',
              render: (details?: Record<string, unknown>) =>
                details && Object.keys(details).length ? (
                  <Typography.Text
                    ellipsis={{ tooltip: JSON.stringify(details) }}
                    style={{ maxWidth: 480, display: 'inline-block' }}
                  >
                    {JSON.stringify(details)}
                  </Typography.Text>
                ) : (
                  '—'
                ),
            },
          ]}
        />
      </SettingSection>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <SettingSection title="连接池" description="来自组件 details 中的连接池字段">
            {poolDetails ? (
              <pre style={{ margin: 0, whiteSpace: 'pre-wrap', fontSize: 12 }}>
                {JSON.stringify(poolDetails, null, 2)}
              </pre>
            ) : (
              <EmptyState description="暂无连接池指标" />
            )}
          </SettingSection>
        </Col>
        <Col xs={24} lg={12}>
          <SettingSection title="近期错误" description="组件探测失败时的进程内错误环">
            {recentErrors != null ? (
              <pre style={{ margin: 0, whiteSpace: 'pre-wrap', fontSize: 12 }}>
                {JSON.stringify(recentErrors, null, 2)}
              </pre>
            ) : (
              <EmptyState description="暂无近期错误" />
            )}
          </SettingSection>
        </Col>
      </Row>
    </div>
  );
}
