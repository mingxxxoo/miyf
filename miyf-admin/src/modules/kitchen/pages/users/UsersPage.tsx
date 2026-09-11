import { useCallback, useEffect, useState } from 'react';
import { Input, Button, Space, Select, Popconfirm, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import SearchForm from '@/components/SearchForm';
import PageTable from '@/components/PageTable';
import { userApi } from '@/api';
import type { User } from '@/types';
import { EmptyState, PageHeader, StatusBadge } from '@/ui';
import { USER_STATUS } from '@/constants/status';
import { notifyError } from '@/api/errors';
import { usePermissionStore } from '@/stores/permissionStore';

export default function UsersPage() {
  const navigate = useNavigate();
  const canUpdate = usePermissionStore((s) => s.hasPermission('user:update'));
  const [loading, setLoading] = useState(false);
  const [actingId, setActingId] = useState<string | null>(null);
  const [data, setData] = useState<User[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [status, setStatus] = useState<string | undefined>();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await userApi.page({
        page,
        pageSize,
        keyword: keyword || undefined,
        status: status || undefined,
      });
      setData(res.records ?? []);
      setTotal(res.total ?? 0);
    } catch (err) {
      setData([]);
      setTotal(0);
      notifyError(err, '暂时无法加载用户列表，请稍后重试');
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, keyword, status]);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const toggleStatus = async (record: User) => {
    if (!canUpdate || actingId) return;
    const next = record.status === 'DISABLED' ? 'ENABLED' : 'DISABLED';
    setActingId(record.id);
    try {
      await userApi.updateStatus(record.id, next);
      message.success(next === 'DISABLED' ? '已禁用登录' : '已恢复登录');
      void fetchData();
    } catch (err) {
      notifyError(err, '更新状态失败');
    } finally {
      setActingId(null);
    }
  };

  return (
    <div className="ck-page">
      <PageHeader
        title="用户管理"
        description="厨房微信用户：可禁用/解禁登录；删除账号请在设置中心 IAM 用户管理"
      />
      <SearchForm
        fields={[
          {
            name: 'keyword',
            label: '用户名 / 手机 / 微信号',
            element: <Input placeholder="搜索用户" allowClear />,
          },
          {
            name: 'status',
            label: '状态',
            element: (
              <Select
                allowClear
                placeholder="全部"
                options={[
                  { value: 'ENABLED', label: '正常' },
                  { value: 'DISABLED', label: '已禁用' },
                ]}
              />
            ),
          },
        ]}
        loading={loading}
        onSearch={(values) => {
          setKeyword(String(values.keyword ?? ''));
          setStatus(values.status ? String(values.status) : undefined);
          setPage(1);
        }}
      />
      <PageTable<User>
        title="用户列表"
        loading={loading}
        columns={[
          { title: '用户名', dataIndex: 'username', render: (v?: string) => v || '—' },
          { title: '昵称', dataIndex: 'nickname' },
          { title: '手机', dataIndex: 'phone', render: (v?: string) => v || '—' },
          { title: '微信号', dataIndex: 'wechatId', render: (v?: string) => v || '—' },
          {
            title: '状态',
            dataIndex: 'status',
            render: (code?: string) => (
              <StatusBadge code={code || 'ENABLED'} map={USER_STATUS} />
            ),
          },
          { title: '注册时间', dataIndex: 'createTime', render: (v?: string) => v || '—' },
          {
            title: '操作',
            key: 'action',
            width: 200,
            render: (_, record) => {
              const disabled = record.status === 'DISABLED';
              return (
                <Space>
                  <Button type="link" onClick={() => navigate(`/kitchen/users/${record.id}`)}>
                    详情
                  </Button>
                  {canUpdate ? (
                    <Popconfirm
                      title={disabled ? '确认恢复该用户登录？' : '确认禁用该用户登录？'}
                      description={
                        disabled
                          ? '解禁后可再次微信登录'
                          : '禁用后微信登录将被拒绝，已发令牌立即失效'
                      }
                      onConfirm={() => void toggleStatus(record)}
                    >
                      <Button type="link" danger={!disabled} loading={actingId === record.id}>
                        {disabled ? '解禁' : '禁用登录'}
                      </Button>
                    </Popconfirm>
                  ) : null}
                </Space>
              );
            },
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
        locale={{ emptyText: <EmptyState description="还没有用户来访厨房哦" /> }}
      />
    </div>
  );
}
