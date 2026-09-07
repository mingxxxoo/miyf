import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Descriptions, Button, Spin, message } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { userApi } from '@/api';
import type { User } from '@/types';
import { EmptyState, PageHeader, StatusBadge } from '@/ui';
import { USER_STATUS } from '@/constants/status';

export default function UserDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    if (!id) return;
    let cancelled = false;
    (async () => {
      setLoading(true);
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
      <PageHeader title="用户详情" />
<Spin spinning={loading}>
        {!user && !loading ? (
          <EmptyState description="未找到该用户" />
        ) : (
          <Card bordered={false}>
            <Descriptions column={2}>
              <Descriptions.Item label="用户名">{user?.username ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="昵称">{user?.nickname ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="手机">{user?.phone ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="微信号">{user?.wechatId ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <StatusBadge code={user?.status || 'ACTIVE'} map={USER_STATUS} />
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
