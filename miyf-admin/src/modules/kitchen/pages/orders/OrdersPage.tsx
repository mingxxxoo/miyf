import { useCallback, useEffect, useState } from 'react';
import { Button, Select, Space, Tag } from 'antd';
import { useNavigate } from 'react-router-dom';
import SearchForm from '@/components/SearchForm';
import PageTable from '@/components/PageTable';
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

export default function OrdersPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<Order[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [status, setStatus] = useState<string | undefined>();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await orderApi.page({ page, pageSize, status });
      setData(res.records ?? []);
      setTotal(res.total ?? 0);
    } catch {
      setData([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, status]);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  return (
    <div className="ck-page">
      <h2 className="ck-page-title">预约管理</h2>
      <SearchForm
        fields={[
          {
            name: 'status',
            label: '状态',
            element: (
              <Select
                allowClear
                placeholder="全部状态"
                options={Object.entries(statusMeta).map(([value, meta]) => ({
                  value,
                  label: meta.text,
                }))}
              />
            ),
          },
        ]}
        loading={loading}
        onSearch={(values) => {
          setStatus(values.status ? String(values.status) : undefined);
          setPage(1);
        }}
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
            render: (s: OrderStatus) => {
              const meta = statusMeta[s] ?? { color: 'default', text: s };
              return <Tag color={meta.color}>{meta.text}</Tag>;
            },
          },
          {
            title: '备注',
            dataIndex: 'remark',
            ellipsis: true,
            render: (v?: string, r?: Order) => v || r?.note || '—',
          },
          {
            title: '创建时间',
            dataIndex: 'createdAt',
            render: (v: string) => String(v || '').replace('T', ' ').slice(0, 16),
          },
          {
            title: '操作',
            key: 'action',
            render: (_, record) => (
              <Space>
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
          onChange: (p, ps) => {
            setPage(p);
            setPageSize(ps);
          },
        }}
        locale={{ emptyText: '暂时没有预约单，厨房很安静' }}
      />
    </div>
  );
}
