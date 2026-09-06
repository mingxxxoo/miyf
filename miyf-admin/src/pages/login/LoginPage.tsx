import { useState } from 'react';

import { useNavigate } from 'react-router-dom';

import { Card, Form, Input, Button, message } from 'antd';

import { UserOutlined, LockOutlined } from '@ant-design/icons';

import { authApi } from '@/api';

import { useAuthStore } from '@/stores/authStore';

import { usePermissionStore } from '@/stores/permissionStore';



export default function LoginPage() {

  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();

  const login = useAuthStore((s) => s.login);

  const setPermissions = usePermissionStore((s) => s.setPermissions);



  const onFinish = async (values: { username: string; password: string }) => {

    setLoading(true);

    try {

      const result = await authApi.login(values.username, values.password);

      login(result.token, result.user, result.permissions);

      setPermissions(result.permissions?.length ? result.permissions : ['*']);

      message.success('欢迎回来，厨房已就绪');

      navigate('/dashboard');

    } catch (err) {

      message.error(err instanceof Error ? err.message : '登录失败，请检查账号密码');

    } finally {

      setLoading(false);

    }

  };



  return (

    <div className="login-page">

      <Card className="login-card" bordered={false}>

        <div className="login-brand">

          <h1>miyf</h1>

          <p>平台管理后台</p>

        </div>

        <Form layout="vertical" onFinish={onFinish} size="large" initialValues={{ username: 'admin' }}>

          <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>

            <Input prefix={<UserOutlined />} placeholder="用户名" autoComplete="username" />

          </Form.Item>

          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>

            <Input.Password

              prefix={<LockOutlined />}

              placeholder="密码"

              autoComplete="current-password"

            />

          </Form.Item>

          <Form.Item>

            <Button type="primary" htmlType="submit" block loading={loading}>

              登录

            </Button>

          </Form.Item>

        </Form>

      </Card>

    </div>

  );

}


