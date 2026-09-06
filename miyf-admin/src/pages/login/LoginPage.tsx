import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Form, Input, Space } from 'antd';
import { LockOutlined, ReloadOutlined, SafetyCertificateOutlined, UserOutlined } from '@ant-design/icons';
import { authApi, type LoginRisk } from '@/api';
import { ApiError } from '@/api/http';
import { useAuthStore } from '@/stores/authStore';
import { usePermissionStore } from '@/stores/permissionStore';

function applyRisk(risk?: LoginRisk | null) {
  if (!risk) {
    return { captchaRequired: false, locked: false, lockRemainSeconds: 0 };
  }
  return {
    captchaRequired: !!risk.captchaRequired,
    locked: !!risk.locked,
    lockRemainSeconds: Number(risk.lockRemainSeconds || 0),
  };
}

export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [captchaRequired, setCaptchaRequired] = useState(false);
  const [locked, setLocked] = useState(false);
  const [lockRemain, setLockRemain] = useState(0);
  const [captchaId, setCaptchaId] = useState('');
  const [captchaImg, setCaptchaImg] = useState('');
  const [captchaLoading, setCaptchaLoading] = useState(false);
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const setPermissions = usePermissionStore((s) => s.setPermissions);
  const [form] = Form.useForm();
  const timerRef = useRef<number | null>(null);

  const clearTimer = () => {
    if (timerRef.current != null) {
      window.clearInterval(timerRef.current);
      timerRef.current = null;
    }
  };

  const startLockCountdown = useCallback((seconds: number) => {
    clearTimer();
    setLockRemain(seconds);
    if (seconds <= 0) return;
    timerRef.current = window.setInterval(() => {
      setLockRemain((prev) => {
        if (prev <= 1) {
          clearTimer();
          setLocked(false);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  }, []);

  useEffect(() => () => clearTimer(), []);

  const refreshCaptcha = async () => {
    setCaptchaLoading(true);
    try {
      const cap = await authApi.captcha();
      setCaptchaId(cap.captchaId);
      setCaptchaImg(cap.imageBase64);
      form.setFieldValue('captchaCode', undefined);
    } catch (err) {
      setFormError(err instanceof Error ? err.message : '验证码加载失败');
    } finally {
      setCaptchaLoading(false);
    }
  };

  const syncRiskFromUsername = async (username?: string) => {
    const name = (username ?? form.getFieldValue('username') ?? '').trim();
    if (!name) return;
    try {
      const risk = await authApi.loginStatus(name);
      const state = applyRisk(risk);
      setCaptchaRequired(state.captchaRequired);
      setLocked(state.locked);
      if (state.locked) {
        startLockCountdown(state.lockRemainSeconds);
      }
      if (state.captchaRequired && !captchaImg) {
        await refreshCaptcha();
      }
    } catch {
      // 状态查询失败不阻断登录
    }
  };

  const handleRiskPayload = async (data: unknown) => {
    const risk = data as LoginRisk | undefined;
    const state = applyRisk(risk);
    setCaptchaRequired(state.captchaRequired);
    setLocked(state.locked);
    if (state.locked) {
      startLockCountdown(state.lockRemainSeconds);
    }
    if (state.captchaRequired) {
      await refreshCaptcha();
    }
  };

  const onFinish = async (values: {
    username: string;
    password: string;
    captchaCode?: string;
  }) => {
    setLoading(true);
    setFormError(null);
    try {
      const result = await authApi.login(values.username, values.password, {
        captchaId: captchaRequired ? captchaId : undefined,
        captchaCode: captchaRequired ? values.captchaCode : undefined,
      });
      login(result.token, result.user, result.permissions);
      setPermissions(result.permissions || []);
      navigate('/dashboard');
    } catch (err) {
      const apiErr = err instanceof ApiError ? err : null;
      const message = apiErr?.message || (err instanceof Error ? err.message : '登录失败，请检查账号密码');
      setFormError(message);
      if (apiErr?.data) {
        await handleRiskPayload(apiErr.data);
      } else if (apiErr?.code === 41011 || apiErr?.code === 41012) {
        setCaptchaRequired(true);
        await refreshCaptcha();
      } else {
        await syncRiskFromUsername(values.username);
      }
    } finally {
      setLoading(false);
    }
  };

  const lockHint =
    locked && lockRemain > 0
      ? `账号已临时锁定，请 ${lockRemain >= 60 ? `${Math.ceil(lockRemain / 60)} 分钟` : `${lockRemain} 秒`} 后再试`
      : null;

  return (
    <div className="login-page">
      <Card className="login-card" bordered={false}>
        <div className="login-brand">
          <h1>miyf</h1>
          <p>平台管理后台</p>
        </div>

        {(formError || lockHint) && (
          <Alert
            className="login-alert"
            type={locked ? 'warning' : 'error'}
            showIcon
            closable={!locked}
            onClose={() => setFormError(null)}
            message={lockHint || formError}
            style={{ marginBottom: 16 }}
          />
        )}

        <Form
          form={form}
          layout="vertical"
          onFinish={onFinish}
          size="large"
          initialValues={{ username: 'admin' }}
          disabled={locked}
        >
          <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input
              prefix={<UserOutlined />}
              placeholder="用户名"
              autoComplete="username"
              onBlur={() => void syncRiskFromUsername()}
            />
          </Form.Item>

          <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="密码"
              autoComplete="current-password"
            />
          </Form.Item>

          {captchaRequired && (
            <Form.Item
              name="captchaCode"
              rules={[{ required: true, message: '请输入验证码' }]}
              extra="连续失败 3 次后需验证码；6 次起将临时锁定账号"
            >
              <Space.Compact style={{ width: '100%' }}>
                <Input
                  prefix={<SafetyCertificateOutlined />}
                  placeholder="验证码"
                  maxLength={6}
                  autoComplete="off"
                />
                <Button
                  type="default"
                  loading={captchaLoading}
                  icon={<ReloadOutlined />}
                  onClick={() => void refreshCaptcha()}
                  style={{ width: 120, padding: 0, height: 40 }}
                >
                  {captchaImg ? (
                    <img
                      src={`data:image/png;base64,${captchaImg}`}
                      alt="captcha"
                      style={{ height: 36, display: 'block', margin: '0 auto' }}
                    />
                  ) : (
                    '获取'
                  )}
                </Button>
              </Space.Compact>
            </Form.Item>
          )}

          <Form.Item>
            <Button type="primary" htmlType="submit" block loading={loading} disabled={locked}>
              {locked ? '已锁定' : '登录'}
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
}
