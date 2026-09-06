import { useCallback, useEffect, useState } from 'react';
import { Button, Card, Col, Progress, Row, Space, Table, Tag, Typography, message } from 'antd';
import { sysMonitorApi, type MonitorOverview } from '@/pages/system/api';

function formatBytes(n?: number) {
  if (n == null || Number.isNaN(n) || n < 0) return '-';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let v = n;
  let i = 0;
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024;
    i += 1;
  }
  return `${v.toFixed(i === 0 ? 0 : 1)} ${units[i]}`;
}

function formatUptime(ms?: number) {
  if (ms == null || ms < 0) return '-';
  const s = Math.floor(ms / 1000);
  const d = Math.floor(s / 86400);
  const h = Math.floor((s % 86400) / 3600);
  const m = Math.floor((s % 3600) / 60);
  if (d > 0) return `${d}天 ${h}时 ${m}分`;
  if (h > 0) return `${h}时 ${m}分`;
  return `${m}分 ${s % 60}秒`;
}

function statusColor(status?: string) {
  if (status === 'UP') return 'success';
  if (status === 'DOWN') return 'error';
  if (status === 'DISABLED') return 'default';
  return 'processing';
}

/**
 * 系统监控：JVM / 磁盘 / Redis / DB。
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
      message.error(err instanceof Error ? err.message : '加载失败');
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
  const heapPct =
    jvm?.heapMax && jvm.heapMax > 0
      ? Math.min(100, Math.round(((jvm.heapUsed ?? 0) / jvm.heapMax) * 100))
      : 0;
  const diskPct =
    disk?.total && disk.total > 0
      ? Math.min(100, Math.round(((disk.total - (disk.free ?? 0)) / disk.total) * 100))
      : 0;

  return (
    <div className="ck-page">
      <Space style={{ width: '100%', justifyContent: 'space-between', marginBottom: 16 }} wrap>
        <h2 className="ck-page-title" style={{ margin: 0 }}>
          系统监控
        </h2>
        <Button loading={loading} onClick={() => void fetchData()}>
          刷新
        </Button>
      </Space>

      <Row gutter={[16, 16]}>
        <Col xs={24} md={12} lg={8}>
          <Card title="JVM 堆内存" loading={loading && !data}>
            <Progress percent={heapPct} status={heapPct > 85 ? 'exception' : 'active'} />
            <Typography.Text type="secondary">
              {formatBytes(jvm?.heapUsed)} / {formatBytes(jvm?.heapMax)}
            </Typography.Text>
            <div style={{ marginTop: 12 }}>
              <div>运行时长：{formatUptime(jvm?.uptimeMs)}</div>
              <div>线程：{jvm?.threadCount ?? '-'}（峰值 {jvm?.peakThreadCount ?? '-'}）</div>
              <div>
                CPU Load：进程{' '}
                {jvm?.processCpuLoad != null && jvm.processCpuLoad >= 0
                  ? `${(jvm.processCpuLoad * 100).toFixed(1)}%`
                  : '-'}{' '}
                / 系统{' '}
                {jvm?.systemCpuLoad != null && jvm.systemCpuLoad >= 0
                  ? `${(jvm.systemCpuLoad * 100).toFixed(1)}%`
                  : '-'}
              </div>
              <div>
                {jvm?.osName ?? '-'} · Java {jvm?.javaVersion ?? '-'}
              </div>
            </div>
          </Card>
        </Col>
        <Col xs={24} md={12} lg={8}>
          <Card title="工作目录磁盘" loading={loading && !data}>
            <Progress percent={diskPct} status={diskPct > 90 ? 'exception' : 'active'} />
            <Typography.Text type="secondary">
              已用 {formatBytes((disk?.total ?? 0) - (disk?.free ?? 0))} / {formatBytes(disk?.total)}
            </Typography.Text>
            <div style={{ marginTop: 12 }}>
              <div>可用：{formatBytes(disk?.usable)}</div>
              <Typography.Paragraph
                type="secondary"
                ellipsis={{ rows: 2, tooltip: disk?.path }}
                style={{ marginBottom: 0 }}
              >
                {disk?.path}
              </Typography.Paragraph>
            </div>
          </Card>
        </Col>
        <Col xs={24} md={24} lg={8}>
          <Card title="采集时间" loading={loading && !data}>
            <Typography.Title level={4} style={{ marginTop: 0 }}>
              {data?.collectedAt ? new Date(data.collectedAt).toLocaleString() : '-'}
            </Typography.Title>
            <Typography.Text type="secondary">每 15 秒自动刷新</Typography.Text>
          </Card>
        </Col>
      </Row>

      <Card title="组件状态" style={{ marginTop: 16 }} loading={loading && !data}>
        <Table
          rowKey="name"
          pagination={false}
          dataSource={data?.components ?? []}
          columns={[
            { title: '组件', dataIndex: 'name', width: 160 },
            {
              title: '状态',
              dataIndex: 'status',
              width: 120,
              render: (v: string) => <Tag color={statusColor(v)}>{v}</Tag>,
            },
            {
              title: '详情',
              dataIndex: 'details',
              render: (details: Record<string, unknown>) =>
                details ? JSON.stringify(details) : '-',
            },
          ]}
        />
      </Card>
    </div>
  );
}
