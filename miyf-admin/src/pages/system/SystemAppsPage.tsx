import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Typography,
  message,
} from 'antd';
import { notifyError } from '@/api/errors';
import { ENABLED_STATUS } from '@/constants/status';
import { useSubmitting } from '@/hooks/useSubmitting';
import { sysAppApi, type SysApp } from '@/modules/system/api';
import {
  EmptyState,
  FormItem,
  FormModal,
  LoadingState,
  PageHeader,
  PageToolbar,
  StatusBadge,
} from '@/ui';

/**
 * 应用管理：卡片网格展示，支持配置与启停。
 */
export default function SystemAppsPage() {
  const [loading, setLoading] = useState(false);
  const [apps, setApps] = useState<SysApp[]>([]);
  const [keyword, setKeyword] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<SysApp | null>(null);
  const [form] = Form.useForm();
  const { submitting, run } = useSubmitting();
  const [statusTargetId, setStatusTargetId] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      setApps(await sysAppApi.list({ keyword: keyword.trim() || undefined }));
    } catch (err) {
      setApps([]);
      notifyError(err, '加载失败');
    } finally {
      setLoading(false);
    }
  }, [keyword]);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ status: 'ENABLED', sortOrder: 0 });
    setOpen(true);
  };

  const openEdit = (row: SysApp) => {
    setEditing(row);
    form.setFieldsValue({
      code: row.code,
      name: row.name,
      description: row.description,
      icon: row.icon,
      homePath: row.homePath,
      sortOrder: row.sortOrder ?? 0,
      status: row.status,
    });
    setOpen(true);
  };

  const handleSubmit = () =>
    run(async () => {
      const values = await form.validateFields();
      try {
        const payload = {
          code: values.code as string,
          name: values.name as string,
          description: values.description as string | undefined,
          icon: values.icon as string | undefined,
          homePath: values.homePath as string | undefined,
          sortOrder: values.sortOrder as number | undefined,
          status: values.status as string,
        };
        if (editing) {
          await sysAppApi.update(editing.id, payload);
          message.success('应用已更新');
        } else {
          await sysAppApi.create(payload);
          message.success('应用已创建');
        }
        setOpen(false);
        await fetchData();
      } catch (err) {
        notifyError(err, '保存失败');
      }
    });

  const toggleStatus = (row: SysApp) =>
    run(async () => {
      setStatusTargetId(row.id);
      try {
        const next = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
        await sysAppApi.update(row.id, {
          code: row.code,
          name: row.name,
          description: row.description,
          icon: row.icon,
          homePath: row.homePath,
          sortOrder: row.sortOrder,
          status: next,
        });
        message.success(next === 'ENABLED' ? '已启用' : '已停用');
        await fetchData();
      } catch (err) {
        notifyError(err, '状态更新失败');
      } finally {
        setStatusTargetId(null);
      }
    });

  return (
    <div className="ck-page">
      <PageHeader title="应用管理" description="管理系统内各业务应用入口" />
      <PageToolbar
        left={
          <Input.Search
            allowClear
            placeholder="搜索编码 / 名称"
            style={{ width: 240 }}
            onSearch={setKeyword}
            onChange={(e) => {
              if (!e.target.value) setKeyword('');
            }}
          />
        }
        right={
          <Button type="primary" onClick={openCreate}>
            新增应用
          </Button>
        }
      />

      {loading && !apps.length ? (
        <LoadingState tip="加载应用中…" />
      ) : !apps.length ? (
        <EmptyState description="暂无应用" actionText="新增应用" onAction={openCreate} />
      ) : (
        <div className="sys-app-grid">
          {apps.map((app) => (
            <Card key={app.id} className="sys-app-card" loading={loading && !apps.length}>
              <Space style={{ width: '100%', justifyContent: 'space-between' }} align="start">
                <div>
                  <Typography.Title level={5} style={{ margin: 0 }}>
                    {app.name}
                  </Typography.Title>
                  <Typography.Text type="secondary">{app.code}</Typography.Text>
                </div>
                <StatusBadge code={app.status} map={ENABLED_STATUS} />
              </Space>
              <Typography.Paragraph
                type="secondary"
                ellipsis={{ rows: 2 }}
                style={{ marginTop: 12, marginBottom: 0, minHeight: 44 }}
              >
                {app.description || '暂无描述'}
              </Typography.Paragraph>
              <div className="sys-app-card-meta">
                <span>入口：{app.homePath || '—'}</span>
                <span>菜单：{app.menuCount ?? 0}</span>
                <span>权限：{app.permissionCount ?? 0}</span>
              </div>
              <Space style={{ marginTop: 16 }}>
                <Button type="link" size="small" onClick={() => openEdit(app)}>
                  配置
                </Button>
                <Button
                  type="link"
                  size="small"
                  loading={submitting && statusTargetId === app.id}
                  onClick={() => void toggleStatus(app)}
                >
                  {app.status === 'ENABLED' ? '停用' : '启用'}
                </Button>
              </Space>
            </Card>
          ))}
        </div>
      )}

      <FormModal
        title={editing ? '配置应用' : '新增应用'}
        open={open}
        form={form}
        confirmLoading={submitting}
        onOk={() => void handleSubmit()}
        onCancel={() => setOpen(false)}
      >
        <FormItem name="code" label="编码" rules={[{ required: true, message: '请输入编码' }]}>
          <Input placeholder="如：kitchen" disabled={!!editing?.builtIn} />
        </FormItem>
        <FormItem name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
          <Input />
        </FormItem>
        <FormItem name="icon" label="图标">
          <Input placeholder="如：CoffeeOutlined" />
        </FormItem>
        <FormItem name="homePath" label="默认入口">
          <Input placeholder="如：/kitchen/categories" />
        </FormItem>
        <FormItem name="sortOrder" label="排序">
          <InputNumber style={{ width: '100%' }} min={0} />
        </FormItem>
        <FormItem name="status" label="状态" rules={[{ required: true }]}>
          <Select
            options={[
              { value: 'ENABLED', label: '启用' },
              { value: 'DISABLED', label: '停用' },
            ]}
          />
        </FormItem>
        <FormItem name="description" label="描述" full>
          <Input.TextArea rows={2} />
        </FormItem>
      </FormModal>
    </div>
  );
}
