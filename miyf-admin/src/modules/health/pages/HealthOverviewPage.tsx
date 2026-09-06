import { useCallback, useEffect, useState } from 'react';
import { Card, Col, Row, Table, Tag, Typography, message } from 'antd';
import { healthApi, type HealthOverview } from '@/modules/health/api';

/**
 * 健康模块概览。
 */
export default function HealthOverviewPage() {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<HealthOverview | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      setData(await healthApi.overview());
    } catch (err) {
      setData(null);
      message.error(err instanceof Error ? err.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  return (
    <div className="ck-page">
      <h2 className="ck-page-title">健康概览</h2>
      <Typography.Paragraph type="secondary">
        通用健康数据管理：主体、采样与可插拔数据源（不绑定具体厂商）。
      </Typography.Paragraph>
      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card loading={loading}>
            <Typography.Text type="secondary">模块</Typography.Text>
            <Typography.Title level={3} style={{ marginTop: 4 }}>
              {data?.enabled ? '已启用' : '未启用'}
            </Typography.Title>
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card loading={loading}>
            <Typography.Text type="secondary">主体数</Typography.Text>
            <Typography.Title level={3} style={{ marginTop: 4 }}>
              {data?.subjectCount ?? 0}
            </Typography.Title>
          </Card>
        </Col>
        <Col xs={24} md={8}>
          <Card loading={loading}>
            <Typography.Text type="secondary">采样数</Typography.Text>
            <Typography.Title level={3} style={{ marginTop: 4 }}>
              {data?.sampleCount ?? 0}
            </Typography.Title>
          </Card>
        </Col>
      </Row>
      <Card title="已注册数据源" style={{ marginTop: 16 }} loading={loading}>
        <Table
          rowKey="code"
          pagination={false}
          dataSource={data?.providers ?? []}
          columns={[
            { title: '编码', dataIndex: 'code', width: 140 },
            { title: '名称', dataIndex: 'displayName' },
            {
              title: '状态',
              dataIndex: 'enabled',
              width: 100,
              render: (v: boolean) =>
                v ? <Tag color="green">启用</Tag> : <Tag>停用</Tag>,
            },
            {
              title: '远程拉取',
              dataIndex: 'supportsRemoteFetch',
              width: 100,
              render: (v: boolean) => (v ? '是' : '否'),
            },
            {
              title: '指标',
              dataIndex: 'supportedMetrics',
              render: (v?: string[]) => (v?.length ? v.join(', ') : '-'),
            },
          ]}
        />
      </Card>
    </div>
  );
}
