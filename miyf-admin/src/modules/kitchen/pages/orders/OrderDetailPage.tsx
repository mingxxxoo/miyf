import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Card, Descriptions, Space, Spin, Steps, Table, message } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { notifyError, orderApi } from '@/api';
import type { Order, OrderStatus } from '@/types';
import { ORDER_NEXT_STATUS, ORDER_STATUS, statusOf } from '@/constants/status';
import { EmptyState, PageHeader, StatusBadge } from '@/ui';

const FLOW: OrderStatus[] = ['PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'COMPLETED'];

export default function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [order, setOrder] = useState<Order | null>(null);
  const [saving, setSaving] = useState(false);

  const load = async () => {
    if (!id) return;
    setLoading(true);
    try {
      const data = await orderApi.detail(id);
      setOrder(data);
    } catch (err) {
      setOrder(null);
      notifyError(err, '无法加载预约详情');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const nextOptions = order ? ORDER_NEXT_STATUS[order.status] ?? [] : [];

  const stepCurrent = useMemo(() => {
    if (!order) return 0;
    if (order.status === 'CANCELLED') return -1;
    const idx = FLOW.indexOf(order.status);
    return idx >= 0 ? idx : 0;
  }, [order]);

  const handleStatus = async (next: OrderStatus) => {
    if (!id) return;
    setSaving(true);
    try {
      const updated = await orderApi.updateStatus(id, next);
      setOrder(updated);
      message.success(`状态已更新为「${statusOf(ORDER_STATUS, next).text}」`);
    } catch (err) {
      notifyError(err, '更新失败');
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
      <PageHeader
        title="预约详情"
        description={order ? `单号 ${order.orderNo}` : undefined}
        extra={
          order && nextOptions.length ? (
            <Space wrap>
              {nextOptions.map((s) => (
                <Button
                  key={s}
                  type={s === 'CANCELLED' ? 'default' : 'primary'}
                  danger={s === 'CANCELLED'}
                  loading={saving}
                  onClick={() => void handleStatus(s as OrderStatus)}
                >
                  {s === 'CANCELLED' ? '取消预约' : `流转到${statusOf(ORDER_STATUS, s).text}`}
                </Button>
              ))}
            </Space>
          ) : null
        }
      />

      <Spin spinning={loading}>
        {!order && !loading ? (
          <EmptyState description="未找到该预约单" actionText="返回列表" onAction={() => navigate('/kitchen/orders')} />
        ) : order ? (
          <>
            <Card bordered={false} style={{ marginBottom: 16 }}>
              {order.status === 'CANCELLED' ? (
                <StatusBadge code="CANCELLED" map={ORDER_STATUS} />
              ) : (
                <Steps
                  size="small"
                  current={stepCurrent}
                  items={FLOW.map((s) => ({
                    title: statusOf(ORDER_STATUS, s).text,
                  }))}
                  style={{ marginBottom: 24 }}
                />
              )}
              <Descriptions column={{ xs: 1, sm: 2 }}>
                <Descriptions.Item label="预约单号">{order.orderNo}</Descriptions.Item>
                <Descriptions.Item label="用户">{order.userNickname ?? '—'}</Descriptions.Item>
                <Descriptions.Item label="状态">
                  <StatusBadge code={order.status} map={ORDER_STATUS} />
                </Descriptions.Item>
                <Descriptions.Item label="创建时间">
                  {String(order.createTime || '').replace('T', ' ').slice(0, 16) || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="更新时间">
                  {String(order.lastModifyTime || '').replace('T', ' ').slice(0, 16) || '—'}
                </Descriptions.Item>
                <Descriptions.Item label="备注" span={2}>
                  {order.remark || order.note || '无'}
                </Descriptions.Item>
              </Descriptions>
            </Card>

            <Card bordered={false} title="菜品明细">
              <Table
                rowKey={(_, i) => String(i)}
                pagination={false}
                dataSource={order.items ?? []}
                columns={[
                  { title: '菜品', dataIndex: 'dishName' },
                  { title: '数量', dataIndex: 'quantity', width: 100 },
                  { title: '备注', dataIndex: 'remark', render: (v?: string) => v || '—' },
                ]}
                locale={{ emptyText: '无菜品明细' }}
              />
            </Card>
          </>
        ) : null}
      </Spin>
    </div>
  );
}
