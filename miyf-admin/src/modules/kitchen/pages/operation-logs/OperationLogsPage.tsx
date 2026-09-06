import { useCallback, useEffect, useState } from 'react';
import { Input } from 'antd';
import SearchForm from '@/components/SearchForm';
import PageTable from '@/components/PageTable';
import { operationLogApi } from '@/api';
import type { OperationLog } from '@/types';

export default function OperationLogsPage() {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<OperationLog[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [keyword, setKeyword] = useState('');

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await operationLogApi.page({ page, pageSize, keyword: keyword || undefined });
      setData(res.records ?? []);
      setTotal(res.total ?? 0);
    } catch {
      setData([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, keyword]);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  return (
    <div className="ck-page">
      <h2 className="ck-page-title">操作日志</h2>
      <SearchForm
        fields={[
          {
            name: 'keyword',
            label: '关键词',
            element: <Input placeholder="操作人 / 动作 / 模块" allowClear />,
          },
        ]}
        loading={loading}
        onSearch={(v) => {
          setKeyword(String(v.keyword ?? ''));
          setPage(1);
        }}
      />
      <PageTable<OperationLog>
        title="日志列表"
        loading={loading}
        columns={[
          { title: '操作人', dataIndex: 'operatorName', render: (v?: string) => v || '—' },
          { title: '模块', dataIndex: 'module', render: (v?: string) => v || '—' },
          { title: '动作', dataIndex: 'action' },
          { title: '详情', dataIndex: 'detail', ellipsis: true, render: (v?: string) => v || '—' },
          { title: 'IP', dataIndex: 'ip', render: (v?: string) => v || '—' },
          { title: '时间', dataIndex: 'createdAt' },
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
        locale={{ emptyText: '暂无操作记录' }}
      />
    </div>
  );
}
