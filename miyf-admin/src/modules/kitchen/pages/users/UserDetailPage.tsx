import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Descriptions, Button, Spin, Space, Popconfirm, message } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { userApi } from '@/api';
import type { User } from '@/types';
import { EmptyState, PageHeader, StatusBadge } from '@/ui';
import { USER_STATUS } from '@/constants/status';
import { notifyError } from '@/api/errors';
import { usePermissionStore } from '@/stores/permissionStore';

export default function UserDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const canUpdate = usePermissionStore((s) => s.hasPermission('user:update'));
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState<User | null>(null);
  const [acting, setActing] = useState(false);

  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    setLoading(true);
    void (async () => {
      try {
        const data = await userApi.detail(id);
        if (!cancelled) setUser(data);
      } catch {
        if (!cancelled) {
          setUser(null);
          message.warning('无法加载用户详情');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [id]);

  const toggleStatus = async () => {
    if (!user || !canUpdate) return;
    const next = user.status === 'DISABLED' ? 'ENABLED' : 'DISABLED';
    setActing(true);
    try {
      const updated = await userApi.updateStatus(user.id, next);
      setUser(updated);
      message.success(next === 'DISABLED' ? '已禁用登录' : '已恢复登录');
    } catch (err) {
      notifyError(err, '更新状态失败');
    } finally {
      setActing(false);
    }
  };

  const disabled = user?.status === 'DISABLED';

  return (
    <div className="ck-page">
      <Button
        type="link"
        icon={<ArrowLeftOutlined />}
        onClick={() => navigate('/kitchen/users')}
        style={{ marginBottom: 8, paddingLeft: 0 }}
      >
        返回用户列表
      </Button>
      <PageHeader title="用户详情" description="业务侧仅支持启停登录，不可删除用户" />
      <Spin spinning={loading}>
        {!user && !loading ? (
          <EmptyState description="未找到该用户" />
        ) : (
          <Card
            bordered={false}
            extra={
              user && canUpdate ? (
                <Space>
                  <Popconfirm
                    title={disabled ? '确认恢复该用户登录？' : '确认禁用该用户登录？'}
                    description={
                      disabled
                        ? '解禁后可再次微信登录'
                        : '禁用后微信登录将被拒绝，已发令牌立即失效'
                    }
                    onConfirm={() => void toggleStatus()}
                  >
                    <Button danger={!disabled} loading={acting}>
                      {disabled ? '解禁登录' : '禁用登录'}
                    </Button>
                  </Popconfirm>
                </Space>
              ) : null
            }
          >
            <Descriptions column={2}>
              <Descriptions.Item label="用户名">{user?.username ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="昵称">{user?.nickname ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="手机">{user?.phone ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="微信号">{user?.wechatId ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <StatusBadge code={user?.status || 'ENABLED'} map={USER_STATUS} />
              </Descriptions.Item>
              <Descriptions.Item label="注册时间">{user?.createTime ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="最近登录">{user?.lastLoginTime ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="用户 ID">{user?.id ?? '—'}</Descriptions.Item>
            </Descriptions>
          </Card>
        )}
      </Spin>
    </div>
  );
}
