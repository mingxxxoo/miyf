import { useCallback, useEffect, useState } from 'react';
import { Button, Space, Tabs, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import PageTable from '@/components/PageTable';
import {
  EmptyState,
  PageHeader,
  PageToolbar,
  StatusBadge,
} from '@/ui';
import { notifyError, orderApi } from '@/api';
import type { Order, OrderStatus } from '@/types';
import { ORDER_STATUS } from '@/constants/status';

type StatusTab = 'ALL' | OrderStatus;

export default function OrdersPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<Order[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [statusTab, setStatusTab] = useState<StatusTab>('ALL');
  const [actingId, setActingId] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await orderApi.page({
        page,
        pageSize,
        status: statusTab === 'ALL' ? undefined : statusTab,
      });
      setData(res.records ?? []);
      setTotal(res.total ?? 0);
    } catch (err) {
      setData([]);
      setTotal(0);
      notifyError(err, '加载预约失败');
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, statusTab]);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const quickConfirm = async (row: Order) => {
    setActingId(row.id);
    try {
      await orderApi.updateStatus(row.id, 'CONFIRMED');
      message.success(`已确认 ${row.orderNo}`);
      void fetchData();
    } catch (err) {
      notifyError(err, '确认失败');
    } finally {
      setActingId(null);
    }
  };

  return (
    <div className="ck-page">
      <PageHeader
        title="预约管理"
        description="按状态处理共享厨房预约，待确认可一键确认"
      />

      <PageToolbar
        left={
          <Tabs
            activeKey={statusTab}
            onChange={(key) => {
              setStatusTab(key as StatusTab);
              setPage(1);
            }}
            size="small"
            tabBarStyle={{ marginBottom: 0 }}
            items={[
              { key: 'ALL', label: '全部' },
              ...Object.entries(ORDER_STATUS).map(([value, meta]) => ({
                key: value,
                label: meta.text,
              })),
            ]}
          />
        }
      />

      <PageTable<Order>
        title="预约列表"
        loading={loading}
        columns={[
          { title: '预约单号', dataIndex: 'orderNo' },
          { title: '用户', dataIndex: 'userNickname', render: (v?: string) => v || '—' },
          {
            title: '菜品',
            key: 'items',
            render: (_, r) => r.items?.map((i) => i.dishName).join('、') || '—',
          },
          {
            title: '状态',
            dataIndex: 'status',
            render: (s: OrderStatus) => <StatusBadge code={s} map={ORDER_STATUS} />,
          },
          {
            title: '备注',
            dataIndex: 'remark',
            ellipsis: true,
            render: (v?: string, r?: Order) => v || r?.note || '—',
          },
          {
            title: '创建时间',
            dataIndex: 'createTime',
            render: (v: string) => String(v || '').replace('T', ' ').slice(0, 16),
          },
          {
            title: '操作',
            key: 'action',
            width: 200,
            render: (_, record) => (
              <Space>
                {record.status === 'PENDING' ? (
                  <Button
                    type="link"
                    loading={actingId === record.id}
                    onClick={() => void quickConfirm(record)}
                  >
                    快速确认
                  </Button>
                ) : null}
                <Button type="link" onClick={() => navigate(`/kitchen/orders/${record.id}`)}>
                  详情
                </Button>
              </Space>
            ),
          },
        ]}
        dataSource={data}
        pagination={{
          current: page,
          pageSize,
          total,
          showSizeChanger: true,
          onChange: (p, ps) => {
            setPage(p);
            setPageSize(ps);
          },
        }}
        locale={{
          emptyText: (
            <EmptyState description="暂时没有预约单，厨房很安静" />
          ),
        }}
      />
    </div>
  );
}
