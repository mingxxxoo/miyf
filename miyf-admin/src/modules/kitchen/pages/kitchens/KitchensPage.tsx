import { useCallback, useEffect, useState } from 'react';
import { Button, Input, Select, Space, message } from 'antd';
import SearchForm from '@/components/SearchForm';
import PageTable from '@/components/PageTable';
import { kitchenApi, notifyError } from '@/api';
import { EmptyState, PageHeader, StatusBadge } from '@/ui';
import { KITCHEN_STATUS } from '@/constants/status';

type KitchenRow = {
  id: string;
  name: string;
  intro?: string;
  status: string;
};

export default function KitchensPage() {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<KitchenRow[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [keyword, setKeyword] = useState<string>();
  const [status, setStatus] = useState<string>();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await kitchenApi.page({ page, pageSize, keyword, status });
      setData(res.records ?? []);
      setTotal(res.total ?? 0);
    } catch (err) {
      setData([]);
      setTotal(0);
      notifyError(err, '加载厨房失败');
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, keyword, status]);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  return (
    <div className="ck-page">
      <PageHeader title="厨房" description="管理厨师个人厨房的营业与封禁状态" />
      <SearchForm
        fields={[
          { name: 'keyword', label: '名称', element: <Input allowClear placeholder="厨房名称" /> },
          {
            name: 'status',
            label: '状态',
            element: (
              <Select
                allowClear
                options={[
                  { value: 'OPEN', label: '营业' },
                  { value: 'CLOSED', label: '停业' },
                  { value: 'BANNED', label: '封禁' },
                ]}
              />
            ),
          },
        ]}
        loading={loading}
        onSearch={(values) => {
          setKeyword(values.keyword ? String(values.keyword) : undefined);
          setStatus(values.status ? String(values.status) : undefined);
          setPage(1);
        }}
      />
      <PageTable<KitchenRow>
        title="厨房列表"
        loading={loading}
        rowKey="id"
        columns={[
          { title: '名称', dataIndex: 'name' },
          { title: '简介', dataIndex: 'intro', render: (v?: string) => v || '—' },
          {
            title: '状态',
            dataIndex: 'status',
            render: (code: string) => <StatusBadge code={code} map={KITCHEN_STATUS} />,
          },
          {
            title: '操作',
            render: (_, row) => (
              <Space>
                {row.status !== 'OPEN' && (
                  <Button
                    type="link"
                    onClick={async () => {
                      try {
                        await kitchenApi.updateStatus(row.id, 'OPEN');
                        message.success('已设为营业');
                        void fetchData();
                      } catch (err) {
                        notifyError(err, '操作失败');
                      }
                    }}
                  >
                    营业
                  </Button>
                )}
                {row.status !== 'CLOSED' && (
                  <Button
                    type="link"
                    onClick={async () => {
                      try {
                        await kitchenApi.updateStatus(row.id, 'CLOSED');
                        message.success('已停业');
                        void fetchData();
                      } catch (err) {
                        notifyError(err, '操作失败');
                      }
                    }}
                  >
                    停业
                  </Button>
                )}
                {row.status !== 'BANNED' && (
                  <Button
                    type="link"
                    danger
                    onClick={async () => {
                      try {
                        await kitchenApi.updateStatus(row.id, 'BANNED');
                        message.success('已封禁');
                        void fetchData();
                      } catch (err) {
                        notifyError(err, '操作失败');
                      }
                    }}
                  >
                    封禁
                  </Button>
                )}
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
        locale={{ emptyText: <EmptyState description="还没有厨房" /> }}
      />
    </div>
  );
}
