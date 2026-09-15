import { useCallback, useEffect, useState } from 'react';
import { Button, Select, message } from 'antd';
import SearchForm from '@/components/SearchForm';
import PageTable from '@/components/PageTable';
import { bindingApi, notifyError } from '@/api';
import { EmptyState, PageHeader, StatusBadge } from '@/ui';
import { BINDING_STATUS } from '@/constants/status';
import { confirmAction } from '@/ui';

type BindingRow = {
  id: string;
  kitchenName: string;
  dinerNickname: string;
  status: string;
  rejectReason?: string;
};

export default function BindingsPage() {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<BindingRow[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [status, setStatus] = useState<string>();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await bindingApi.page({ page, pageSize, status });
      setData(res.records ?? []);
      setTotal(res.total ?? 0);
    } catch (err) {
      setData([]);
      setTotal(0);
      notifyError(err, '加载绑定失败');
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, status]);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  return (
    <div className="ck-page">
      <PageHeader title="绑定关系" description="监管厨师与食客绑定；解除后保留历史预约与评价" />
      <SearchForm
        fields={[
          {
            name: 'status',
            label: '状态',
            element: (
              <Select
                allowClear
                options={[
                  { value: 'PENDING', label: '待确认' },
                  { value: 'BOUND', label: '已绑定' },
                  { value: 'REJECTED', label: '已拒绝' },
                  { value: 'UNBOUND', label: '已解除' },
                ]}
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
      <PageTable<BindingRow>
        title="绑定列表"
        loading={loading}
        rowKey="id"
        columns={[
          { title: '厨房', dataIndex: 'kitchenName' },
          { title: '食客', dataIndex: 'dinerNickname' },
          {
            title: '状态',
            dataIndex: 'status',
            render: (code: string) => <StatusBadge code={code} map={BINDING_STATUS} />,
          },
          { title: '原因', dataIndex: 'rejectReason', render: (v?: string) => v || '—' },
          {
            title: '操作',
            render: (_, row) =>
              row.status === 'BOUND' || row.status === 'PENDING' ? (
                <Button
                  type="link"
                  danger
                  onClick={() =>
                    confirmAction({
                      title: '强制解除',
                      content: '确定解除该绑定吗？历史预约与评价会保留。',
                      onOk: async () => {
                        try {
                          await bindingApi.unbind(row.id);
                          message.success('已解除');
                          void fetchData();
                        } catch (err) {
                          notifyError(err, '操作失败');
                        }
                      },
                    })
                  }
                >
                  解除
                </Button>
              ) : null,
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
        locale={{ emptyText: <EmptyState description="暂无绑定记录" /> }}
      />
    </div>
  );
}
