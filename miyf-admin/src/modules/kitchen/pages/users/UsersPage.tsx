import { useCallback, useEffect, useState } from 'react';
import { Input, Button, Space, Tag, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import SearchForm from '@/components/SearchForm';
import PageTable from '@/components/PageTable';
import { userApi } from '@/api';
import type { User } from '@/types';

export default function UsersPage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<User[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [keyword, setKeyword] = useState('');

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await userApi.page({ page, pageSize, keyword: keyword || undefined });
      setData(res.records ?? []);
      setTotal(res.total ?? 0);
    } catch {
      setData([]);
      setTotal(0);
      message.warning('暂时无法加载用户列表，请稍后重试');
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, keyword]);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  return (
    <div className="ck-page">
      <h2 className="ck-page-title">用户管理</h2>
      <SearchForm
        fields={[
          {
            name: 'keyword',
            label: '昵称 / 手机',
            element: <Input placeholder="搜索用户" allowClear />,
          },
        ]}
        loading={loading}
        onSearch={(values) => {
          setKeyword(String(values.keyword ?? ''));
          setPage(1);
        }}
      />
      <PageTable<User>
        title="用户列表"
        loading={loading}
        columns={[
          { title: '昵称', dataIndex: 'nickname' },
          { title: '手机', dataIndex: 'phone', render: (v?: string) => v || '—' },
          {
            title: '状态',
            dataIndex: 'status',
            render: (status?: string) =>
              status === 'DISABLED' ? <Tag>已停用</Tag> : <Tag color="success">正常</Tag>,
          },
          { title: '注册时间', dataIndex: 'createdAt', render: (v?: string) => v || '—' },
          {
            title: '操作',
            key: 'action',
            render: (_, record) => (
              <Space>
                <Button type="link" onClick={() => navigate(`/kitchen/users/${record.id}`)}>
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
        locale={{ emptyText: '还没有用户来访厨房哦' }}
      />
    </div>
  );
}
