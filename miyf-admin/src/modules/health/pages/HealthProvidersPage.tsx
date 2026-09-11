import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import { useSearchParams } from 'react-router-dom';
import { PageHeader } from '@/ui';
import { notifyError } from '@/api/errors';
import { useSubmitting } from '@/hooks/useSubmitting';
import {
  healthApi,
  type HealthBinding,
  type HealthProvider,
  type HealthSubject,
  type HealthSyncRun,
} from '@/modules/health/api';
import { usePermissionStore } from '@/stores/permissionStore';

/** 同一 OAuth code 只换票一次；StrictMode 双挂载共享 Promise，避免丢成功态或重复换票。 */
const huaweiOAuthExchanges = new Map<
  string,
  Promise<{ subjectId?: string; authorized: boolean; openId?: string; expiresTime?: string }>
>();

/**
 * 健康数据源：列表、主体绑定、华为 OAuth、触发同步、运行记录。
 * embedded=true 时用于系统管理中心嵌套，不重复外层标题。
 */
export default function HealthProvidersPage({ embedded = false }: { embedded?: boolean }) {
  const [searchParams, setSearchParams] = useSearchParams();
  const canHuaweiOAuth = usePermissionStore((s) => s.hasPermission('health:huawei:oauth'));
  const [loading, setLoading] = useState(false);
  const [providers, setProviders] = useState<HealthProvider[]>([]);
  const [subjects, setSubjects] = useState<HealthSubject[]>([]);
  const [subjectId, setSubjectId] = useState<string | undefined>();
  const [bindings, setBindings] = useState<HealthBinding[]>([]);
  const [runs, setRuns] = useState<HealthSyncRun[]>([]);
  const [huaweiAuth, setHuaweiAuth] = useState(false);
  const [bindOpen, setBindOpen] = useState(false);
  const [syncOpen, setSyncOpen] = useState(false);
  const [syncProvider, setSyncProvider] = useState<string | undefined>();
  const [bindForm] = Form.useForm();
  const [syncForm] = Form.useForm();
  const { submitting, run } = useSubmitting();

  const refreshProviders = useCallback(async () => {
    setProviders(await healthApi.listProviders());
  }, []);

  const refreshSubjects = useCallback(async () => {
    setSubjects(await healthApi.listSubjects());
  }, []);

  const refreshBindings = useCallback(async () => {
    if (!subjectId) {
      setBindings([]);
      return;
    }
    setBindings(await healthApi.listBindings(subjectId));
  }, [subjectId]);

  const refreshRuns = useCallback(async () => {
    setRuns(
      await healthApi.listSyncRuns({
        subjectId,
        limit: 50,
      }),
    );
  }, [subjectId]);

  const refreshHuaweiStatus = useCallback(async () => {
    if (!subjectId) {
      setHuaweiAuth(false);
      return;
    }
    if (!canHuaweiOAuth) {
      setHuaweiAuth(false);
      return;
    }
    try {
      const st = await healthApi.huaweiOAuthStatus(subjectId);
      setHuaweiAuth(st.authorized);
    } catch (err) {
      setHuaweiAuth(false);
      notifyError(err, '查询华为授权状态失败');
    }
  }, [subjectId, canHuaweiOAuth]);

  const bootstrap = useCallback(async () => {
    setLoading(true);
    try {
      await Promise.all([refreshProviders(), refreshSubjects()]);
      await Promise.all([refreshBindings(), refreshRuns(), refreshHuaweiStatus()]);
    } catch (err) {
      notifyError(err, '加载失败');
    } finally {
      setLoading(false);
    }
  }, [refreshProviders, refreshSubjects, refreshBindings, refreshRuns, refreshHuaweiStatus]);

  useEffect(() => {
    void bootstrap();
  }, [bootstrap]);

  useEffect(() => {
    void (async () => {
      try {
        await refreshBindings();
        await refreshRuns();
        await refreshHuaweiStatus();
      } catch {
        /* ignore */
      }
    })();
  }, [refreshBindings, refreshRuns, refreshHuaweiStatus]);

  useEffect(() => {
    const oauthError = searchParams.get('error');
    const oauthErrorDesc = searchParams.get('error_description');
    if (oauthError) {
      message.error(
        oauthErrorDesc
          ? `华为授权失败：${oauthErrorDesc}`
          : `华为授权失败：${oauthError}`,
      );
      setSearchParams({}, { replace: true });
      return;
    }
    const code = searchParams.get('code');
    const state = searchParams.get('state');
    if (!code) return;
    let alive = true;
    let exchange = huaweiOAuthExchanges.get(code);
    if (!exchange) {
      exchange = healthApi.huaweiOAuthCallback({
        subjectId: subjectId || undefined,
        code,
        state: state || undefined,
      });
      huaweiOAuthExchanges.set(code, exchange);
    }
    exchange
      .then(async (result) => {
        if (!alive) return;
        message.success('华为授权成功');
        setSearchParams({}, { replace: true });
        if (result.subjectId) setSubjectId(result.subjectId);
        await refreshBindings();
        await refreshHuaweiStatus();
      })
      .catch((err) => {
        if (!alive) return;
        notifyError(err, '华为授权失败');
        setSearchParams({}, { replace: true });
      });
    return () => {
      alive = false;
    };
    // 刻意不依赖 subjectId，避免 setSubjectId 触发重复换票
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams, setSearchParams, refreshBindings, refreshHuaweiStatus]);

  const onBind = async () => {
    if (!subjectId) {
      message.warning('请先选择主体');
      return;
    }
    const values = await bindForm.validateFields();
    await run(async () => {
      try {
        await healthApi.upsertBinding(subjectId, values);
        message.success('绑定已保存');
        setBindOpen(false);
        bindForm.resetFields();
        await refreshBindings();
      } catch (err) {
        notifyError(err, '保存失败');
      }
    });
  };

  const onSync = async () => {
    const values = await syncForm.validateFields();
    const code = syncProvider || values.providerCode;
    await run(async () => {
      try {
        const runResult = await healthApi.sync(code, {
          subjectId: values.subjectId,
        });
        message.success(`同步结束：${runResult.status}`);
        setSyncOpen(false);
        await refreshRuns();
        await refreshBindings();
      } catch (err) {
        notifyError(err, '同步失败');
      }
    });
  };

  const onHuaweiAuthorize = async () => {
    if (!canHuaweiOAuth) {
      message.warning('缺少 health:huawei:oauth 权限');
      return;
    }
    if (!subjectId) {
      message.warning('请先选择主体');
      return;
    }
    await run(async () => {
      try {
        const { authorizeUrl } = await healthApi.huaweiAuthorizeUrl(subjectId);
        if (!authorizeUrl) {
          message.error('未返回授权地址');
          return;
        }
        window.location.href = authorizeUrl;
      } catch (err) {
        notifyError(err, '获取授权地址失败');
      }
    });
  };

  const onHuaweiRevoke = async () => {
    if (!canHuaweiOAuth || !subjectId) return;
    await run(async () => {
      try {
        await healthApi.huaweiRevoke(subjectId);
        message.success('已撤销该主体的华为授权');
        setHuaweiAuth(false);
        await refreshBindings();
      } catch (err) {
        notifyError(err, '撤销失败');
      }
    });
  };

  const huaweiEnabled = providers.some((p) => p.code === 'huawei' && p.enabled);

  return (
    <div className={embedded ? undefined : 'ck-page'}>
      {embedded ? null : <PageHeader title="健康数据源" />}
      <Typography.Paragraph type="secondary">
        管理可插拔 Provider（含华为运动健康 OAuth）、主体绑定与同步。manual 源同步会记为 SKIPPED。
      </Typography.Paragraph>

      <Card title="已注册 Provider" loading={loading} style={{ marginBottom: 16 }}>
        <Table
          rowKey="code"
          pagination={false}
          dataSource={providers}
          columns={[
            { title: '编码', dataIndex: 'code', width: 140 },
            { title: '名称', dataIndex: 'displayName' },
            {
              title: '启用',
              dataIndex: 'enabled',
              width: 90,
              render: (v: boolean) =>
                v ? <Tag color="green">是</Tag> : <Tag>否</Tag>,
            },
            {
              title: '远程拉取',
              dataIndex: 'supportsRemoteFetch',
              width: 100,
              render: (v: boolean) => (v ? '是' : '否'),
            },
            {
              title: '操作',
              width: 120,
              render: (_, row) => (
                <Button
                  type="link"
                  size="small"
                  disabled={!row.enabled}
                  onClick={() => {
                    setSyncProvider(row.code);
                    syncForm.setFieldsValue({
                      providerCode: row.code,
                      subjectId,
                    });
                    setSyncOpen(true);
                  }}
                >
                  同步
                </Button>
              ),
            },
          ]}
        />
      </Card>

      <Card
        title="华为运动健康（仅 OAuth）"
        style={{ marginBottom: 16 }}
        extra={
          <Space wrap>
            <Select
              allowClear
              placeholder="选择主体"
              style={{ width: 200 }}
              value={subjectId}
              onChange={setSubjectId}
              options={subjects.map((s) => ({ value: s.id, label: s.displayName }))}
            />
            <Tag color={huaweiAuth ? 'green' : 'default'}>
              {!canHuaweiOAuth ? '无权限' : huaweiAuth ? '已授权' : '未授权'}
            </Tag>
            <Button
              type="primary"
              disabled={!subjectId || !huaweiEnabled || !canHuaweiOAuth}
              onClick={() => void onHuaweiAuthorize()}
            >
              OAuth 授权
            </Button>
            <Popconfirm
              title="确认撤销该主体的华为授权？"
              description="撤销后需重新授权才能同步"
              disabled={!subjectId || !huaweiAuth || !canHuaweiOAuth}
              onConfirm={() => void onHuaweiRevoke()}
            >
              <Button disabled={!subjectId || !huaweiAuth || !canHuaweiOAuth}>撤销</Button>
            </Popconfirm>
          </Space>
        }
      >
        <Typography.Paragraph type="secondary" style={{ marginBottom: 0 }}>
          选择健康主体后跳转华为账号登录授权；每个主体独立授权，同步只拉该主体数据。
          需配置 app.health.providers.huawei.enabled 与 client-id / client-secret / redirect-uri。
          {!huaweiEnabled ? ' 当前华为 Provider 未启用。' : ''}
          {!canHuaweiOAuth ? ' 当前账号缺少 health:huawei:oauth 权限。' : ''}
        </Typography.Paragraph>
      </Card>

      <Card
        title="主体绑定"
        extra={
          <Space>
            <Select
              allowClear
              placeholder="选择主体"
              style={{ width: 200 }}
              value={subjectId}
              onChange={setSubjectId}
              options={subjects.map((s) => ({ value: s.id, label: s.displayName }))}
            />
            <Button
              type="primary"
              disabled={!subjectId}
              onClick={() => {
                bindForm.resetFields();
                bindForm.setFieldsValue({ status: 'ACTIVE', providerCode: 'huawei' });
                setBindOpen(true);
              }}
            >
              绑定/更新
            </Button>
          </Space>
        }
        style={{ marginBottom: 16 }}
      >
        <Table
          rowKey="id"
          pagination={false}
          dataSource={bindings}
          locale={{ emptyText: subjectId ? '暂无绑定' : '请先选择主体' }}
          columns={[
            { title: '数据源', dataIndex: 'providerCode', width: 140 },
            { title: '名称', dataIndex: 'providerDisplayName', width: 140 },
            { title: '外部账号', dataIndex: 'externalAccountMasked' },
            {
              title: '凭证',
              dataIndex: 'hasCredential',
              width: 90,
              render: (v: boolean) => (v ? '已配置' : '未配置'),
            },
            { title: '状态', dataIndex: 'status', width: 110 },
            { title: '最近同步', dataIndex: 'lastSyncTime', width: 200 },
          ]}
        />
      </Card>

      <Card title="同步记录" extra={<Button onClick={() => void refreshRuns()}>刷新</Button>}>
        <Table
          rowKey="id"
          dataSource={runs}
          pagination={{ pageSize: 10 }}
          columns={[
            { title: '数据源', dataIndex: 'providerCode', width: 120 },
            { title: '主体', dataIndex: 'subjectId', width: 160 },
            {
              title: '状态',
              dataIndex: 'status',
              width: 110,
              render: (v: string) => {
                const color =
                  v === 'SUCCESS'
                    ? 'green'
                    : v === 'FAILED'
                      ? 'red'
                      : v === 'SKIPPED'
                        ? 'default'
                        : 'blue';
                return <Tag color={color}>{v}</Tag>;
              },
            },
            { title: '拉取', dataIndex: 'fetchedCount', width: 80 },
            { title: '入库', dataIndex: 'ingestedCount', width: 80 },
            { title: '开始', dataIndex: 'startedTime', width: 180 },
            { title: '结束', dataIndex: 'finishedTime', width: 180 },
            { title: '错误', dataIndex: 'errorMessage', ellipsis: true },
          ]}
        />
      </Card>

      <Modal
        title="绑定数据源"
        open={bindOpen}
        onCancel={() => setBindOpen(false)}
        onOk={() => void onBind()}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form form={bindForm} layout="vertical">
          <Form.Item name="providerCode" label="数据源" rules={[{ required: true }]}>
            <Select
              options={providers.map((p) => ({
                value: p.code,
                label: `${p.displayName} (${p.code})`,
              }))}
            />
          </Form.Item>
          <Form.Item name="externalAccountId" label="外部账号 ID">
            <Input />
          </Form.Item>
          <Form.Item name="credentialRef" label="凭证引用">
            <Input placeholder="不落明文密钥，仅存引用" />
          </Form.Item>
          <Form.Item name="status" label="状态" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'ACTIVE', label: 'ACTIVE' },
                { value: 'INACTIVE', label: 'INACTIVE' },
                { value: 'REVOKED', label: 'REVOKED' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="触发同步"
        open={syncOpen}
        onCancel={() => setSyncOpen(false)}
        onOk={() => void onSync()}
        confirmLoading={submitting}
        destroyOnClose
      >
        <Form form={syncForm} layout="vertical">
          <Form.Item name="providerCode" label="数据源" rules={[{ required: true }]}>
            <Select
              options={providers.map((p) => ({
                value: p.code,
                label: `${p.displayName} (${p.code})`,
              }))}
            />
          </Form.Item>
          <Form.Item name="subjectId" label="主体" rules={[{ required: true }]}>
            <Select options={subjects.map((s) => ({ value: s.id, label: s.displayName }))} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
