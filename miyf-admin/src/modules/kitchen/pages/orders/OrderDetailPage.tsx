import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Card, Descriptions, Empty, Select, Space, Spin, Table, Tag, message } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { orderApi } from '@/api';
import type { Order, OrderStatus } from '@/types';

const statusMeta: Record<OrderStatus, { color: string; text: string }> = {
  PENDING: { color: 'gold', text: '待确认' },
  CONFIRMED: { color: 'blue', text: '已确认' },
  PREPARING: { color: 'processing', text: '准备中' },
  READY: { color: 'cyan', text: '待取餐' },
  COMPLETED: { color: 'success', text: '已完成' },
  CANCELLED: { color: 'default', text: '已取消' },
};

const nextStatus: Partial<Record<OrderStatus, OrderStatus[]>> = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['PREPARING'],
  PREPARING: ['READY'],
  READY: ['COMPLETED'],
};

export default function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [order, setOrder] = useState<Order | null>(null);
  const [next, setNext] = useState<OrderStatus | undefined>();
  const [saving, setSaving] = useState(false);

  const load = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const data = await orderApi.detail(id);
      setOrder(data);
      setNext(undefined);
    } catch {
      setOrder(null);
      message.warning('无法加载预约详情');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const handleStatus = async () => {
    if (!id || !next) return;
    setSaving(true);
    try {
      const updated = await orderApi.updateStatus(id, next);
      setOrder(updated);
      message.success('状态已更新');
    } catch (err) {
      message.error(err instanceof Error ? err.message : '更新失败');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="ck-page">
      <Button
        type="link"
        icon={<ArrowLeftOutlined />}
        onClick={() => navigate('/kitchen/orders')}
        style={{ marginBottom: 8, paddingLeft: 0 }}
      >
        返回预约列表
      </Button>
      <h2 className="ck-page-title">预约详情</h2>
      <Spin spinning={loading}>
        {!order && !loading ? (
          <Empty description="未找到该预约单" />
        ) : (
          <>
            <Card bordered={false} style={{ marginBottom: 16 }}>
              <Descriptions column={2}>
                <Descriptions.Item label="预约单号">{order?.orderNo}</Descriptions.Item>
                <Descriptions.Item label="用户">{order?.userNickname ?? '—'}</Descriptions.Item>
                <Descriptions.Item label="状态">
                  {order && (
                    <Tag color={statusMeta[order.status]?.color}>
                      {statusMeta[order.status]?.text ?? order.status}
                    </Tag>
                  )}
                </Descriptions.Item>
                <Descriptions.Item label="创建时间">
                  {String(order?.createdAt || '').replace('T', ' ').slice(0, 16) || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="更新时间">
                  {String(order?.updatedAt || '').replace('T', ' ').slice(0, 16) || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="备注" span={2}>
                  {order?.remark || order?.note || '无'}
                </Descriptions.Item>
              </Descriptions>
              {order && nextStatus[order.status]?.length ? (
                <Space style={{ marginTop: 16 }}>
                  <Select
                    placeholder="流转到"
                    style={{ width: 160 }}
                    value={next}
                    onChange={setNext}
                    options={(nextStatus[order.status] ?? []).map((s) => ({
                      value: s,
                      label: statusMeta[s].text,
                    }))}
                  />
                  <Button type="primary" disabled={!next} loading={saving} onClick={() => void handleStatus()}>
                    更新状态
                  </Button>
                </Space>
              ) : null}
            </Card>
            <Card title="预约菜品" bordered={false}>
              <Table
                rowKey={(r) => `${r.dishId}-${r.dishName}`}
                pagination={false}
                dataSource={order?.items ?? []}
                columns={[
                  { title: '菜品', dataIndex: 'dishName' },
                  { title: '份数', dataIndex: 'quantity', width: 100 },
                  {
                    title: '备注',
                    key: 'note',
                    render: (_, r) => r.note || r.remark || '—',
                  },
                ]}
                locale={{ emptyText: '无菜品明细' }}
              />
            </Card>
          </>
        )}
      </Spin>
    </div>
  );
}
