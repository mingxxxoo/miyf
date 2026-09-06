import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, Descriptions, Button, Spin, Empty, message } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { userApi } from '@/api';
import type { User } from '@/types';

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
      <h2 className="ck-page-title">用户详情</h2>
      <Spin spinning={loading}>
        {!user && !loading ? (
          <Empty description="未找到该用户" />
        ) : (
          <Card bordered={false}>
            <Descriptions column={2}>
              <Descriptions.Item label="昵称">{user?.nickname ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="手机">{user?.phone ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="状态">
                {user?.status === 'DISABLED' ? '已停用' : '正常'}
              </Descriptions.Item>
              <Descriptions.Item label="注册时间">{user?.createdAt ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="最近登录">{user?.lastLoginAt ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="用户 ID">{user?.id ?? '—'}</Descriptions.Item>
            </Descriptions>
          </Card>
        )}
      </Spin>
    </div>
  );
}
