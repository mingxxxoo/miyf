import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button,
  Col,
  Form,
  Input,
  Popconfirm,
  Row,
  Select,
  Space,
  Table,
  Tabs,
  Typography,
  message,
} from 'antd';
import { notifyError } from '@/api/errors';
import { ENABLED_STATUS, type StatusMeta } from '@/constants/status';
import { useSubmitting } from '@/hooks/useSubmitting';
import {
  sysNotificationApi,
  type InboxItem,
  type NotifySendLog,
  type NotifyTemplate,
} from '@/modules/system/api';
import { useAuthStore } from '@/stores/authStore';
import {
  DetailDrawer,
  EmptyState,
  FormDrawer,
  FormItem,
  FormModal,
  MetricCard,
  PageHeader,
  PageToolbar,
  SettingSection,
  StatusBadge,
} from '@/ui';

const READ_STATUS: Record<string, StatusMeta> = {
  READ: { color: 'default', text: '已读' },
  UNREAD: { color: 'processing', text: '未读' },
};

const SEND_STATUS: Record<string, StatusMeta> = {
  SUCCESS: { color: 'success', text: '成功' },
  FAILED: { color: 'error', text: '失败' },
};

const CHANNEL_OPTIONS = [
  { value: 'INBOX', label: '站内信' },
  { value: 'EMAIL', label: '邮件' },
  { value: 'SMS', label: '短信' },
];

/**
 * 通知中心：发送 / 站内信 / 模板 / 发送历史。
 */
export default function SystemNotifyPage() {
  const authUsername = useAuthStore((s) => s.user?.username);
  const [loading, setLoading] = useState(false);
  const [items, setItems] = useState<InboxItem[]>([]);
  const [templates, setTemplates] = useState<NotifyTemplate[]>([]);
  const [logs, setLogs] = useState<NotifySendLog[]>([]);
  const [userKey, setUserKey] = useState(authUsername || '');
  const [sendOpen, setSendOpen] = useState(false);
  const [templateOpen, setTemplateOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<NotifyTemplate | null>(null);
  const [detail, setDetail] = useState<InboxItem | null>(null);
  const [logDetail, setLogDetail] = useState<NotifySendLog | null>(null);
  const [form] = Form.useForm();
  const [templateForm] = Form.useForm();
  const { submitting, run } = useSubmitting();
  const sendMode = Form.useWatch('mode', form) as string | undefined;
  const selectedTemplateCode = Form.useWatch('templateCode', form) as string | undefined;

  useEffect(() => {
    if (authUsername && !userKey) {
      setUserKey(authUsername);
    }
  }, [authUsername, userKey]);

  const fetchInbox = useCallback(async () => {
    if (!userKey.trim()) {
      message.warning('请输入 userKey');
      return;
    }
    setLoading(true);
    try {
      setItems(await sysNotificationApi.listInbox(userKey.trim()));
    } catch (err) {
      setItems([]);
      notifyError(err, '加载失败');
    } finally {
      setLoading(false);
    }
  }, [userKey]);

  const fetchTemplates = useCallback(async () => {
    try {
      setTemplates(await sysNotificationApi.listTemplates());
    } catch (err) {
      setTemplates([]);
      notifyError(err, '模板加载失败');
    }
  }, []);

  const fetchLogs = useCallback(async () => {
    try {
      setLogs(await sysNotificationApi.listSendLogs({ limit: 100 }));
    } catch (err) {
      setLogs([]);
      notifyError(err, '发送历史加载失败');
    }
  }, []);

  useEffect(() => {
    void fetchInbox();
  }, [fetchInbox]);

  useEffect(() => {
    void fetchTemplates();
    void fetchLogs();
  }, [fetchTemplates, fetchLogs]);

  const total = items.length;
  const unread = useMemo(() => items.filter((i) => !i.read).length, [items]);
  const enabledTemplates = useMemo(
    () => templates.filter((t) => t.status === 'ENABLED'),
    [templates],
  );

  const onSend = () =>
    run(async () => {
      const values = await form.validateFields();
      try {
        const mode = (values.mode as string) || 'direct';
        if (mode === 'template') {
          let vars: Record<string, string> | undefined;
          const varsRaw = (values.varsJson as string | undefined)?.trim();
          if (varsRaw) {
            try {
              const parsed = JSON.parse(varsRaw) as unknown;
              if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
                message.error('变量须为 JSON 对象，如 {"name":"张三"}');
                return;
              }
              vars = Object.fromEntries(
                Object.entries(parsed as Record<string, unknown>).map(([k, v]) => [
                  k,
                  v == null ? '' : String(v),
                ]),
              );
            } catch {
              message.error('变量 JSON 格式无效');
              return;
            }
          }
          await sysNotificationApi.send({
            to: values.to as string,
            templateCode: values.templateCode as string,
            vars,
          });
        } else {
          await sysNotificationApi.send({
            channel: values.channel as string,
            to: values.to as string,
            title: values.title as string,
            content: values.content as string,
          });
        }
        message.success('已发送');
        setSendOpen(false);
        form.resetFields();
        if (values.to) {
          setUserKey(values.to as string);
        }
        await Promise.all([fetchInbox(), fetchLogs()]);
      } catch (err) {
        notifyError(err, '发送失败');
        void fetchLogs();
      }
    });

  const onMarkRead = (id: string) =>
    run(async () => {
      try {
        await sysNotificationApi.markRead(id, userKey.trim());
        message.success('已标记已读');
        await fetchInbox();
      } catch (err) {
        notifyError(err, '操作失败');
      }
    });

  const openCreateTemplate = () => {
    setEditingTemplate(null);
    templateForm.resetFields();
    templateForm.setFieldsValue({ channel: 'INBOX', status: 'ENABLED' });
    setTemplateOpen(true);
  };

  const openEditTemplate = (row: NotifyTemplate) => {
    setEditingTemplate(row);
    templateForm.setFieldsValue({
      code: row.code,
      name: row.name,
      channel: row.channel,
      titleTemplate: row.titleTemplate,
      contentTemplate: row.contentTemplate,
      status: row.status,
    });
    setTemplateOpen(true);
  };

  const onSaveTemplate = () =>
    run(async () => {
      const values = await templateForm.validateFields();
      const payload = {
        code: values.code as string,
        name: values.name as string,
        channel: values.channel as string,
        titleTemplate: values.titleTemplate as string,
        contentTemplate: values.contentTemplate as string,
        status: values.status as string,
      };
      try {
        if (editingTemplate) {
          await sysNotificationApi.updateTemplate(editingTemplate.id, payload);
          message.success('模板已更新');
        } else {
          await sysNotificationApi.createTemplate(payload);
          message.success('模板已创建');
        }
        setTemplateOpen(false);
        await fetchTemplates();
      } catch (err) {
        notifyError(err, '保存失败');
      }
    });

  const onDeleteTemplate = (id: string) =>
    run(async () => {
      try {
        await sysNotificationApi.removeTemplate(id);
        message.success('已删除');
        await fetchTemplates();
      } catch (err) {
        notifyError(err, '删除失败');
      }
    });

  return (
    <div className="ck-page">
      <PageHeader
        title="通知中心"
        description="发送通知、管理模板与查看发送历史"
        extra={
          <Space>
            <Button onClick={openCreateTemplate}>新建模板</Button>
            <Button
              type="primary"
              onClick={() => {
                form.resetFields();
                form.setFieldsValue({
                  mode: 'direct',
                  channel: 'INBOX',
                  to: userKey,
                });
                setSendOpen(true);
              }}
            >
              发送通知
            </Button>
          </Space>
        }
      />

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={12} md={6}>
          <MetricCard label="站内信总数" value={total} hint={`userKey: ${userKey || '—'}`} />
        </Col>
        <Col xs={12} md={6}>
          <MetricCard label="未读" value={unread} />
        </Col>
        <Col xs={12} md={6}>
          <MetricCard label="模板数" value={templates.length} />
        </Col>
        <Col xs={12} md={6}>
          <MetricCard label="近期发送" value={logs.length} hint="最近 100 条" />
        </Col>
      </Row>

      <Tabs
        items={[
          {
            key: 'inbox',
            label: '站内信',
            children: (
              <>
                <PageToolbar
                  left={
                    <Space>
                      <Input
                        value={userKey}
                        onChange={(e) => setUserKey(e.target.value)}
                        placeholder="userKey"
                        style={{ width: 200 }}
                        onPressEnter={() => void fetchInbox()}
                      />
                      <Button loading={loading} onClick={() => void fetchInbox()}>
                        查询
                      </Button>
                    </Space>
                  }
                />
                <SettingSection title="收件箱" description="按 userKey 查询站内信">
                  <Table<InboxItem>
                    rowKey="id"
                    loading={loading}
                    dataSource={items}
                    pagination={false}
                    locale={{ emptyText: <EmptyState description="暂无站内信" /> }}
                    columns={[
                      { title: '标题', dataIndex: 'title', ellipsis: true },
                      {
                        title: '状态',
                        width: 100,
                        render: (_, row) => (
                          <StatusBadge code={row.read ? 'READ' : 'UNREAD'} map={READ_STATUS} />
                        ),
                      },
                      { title: '时间', dataIndex: 'createTime', width: 180, render: (v?: string) => v || '—' },
                      {
                        title: '操作',
                        width: 140,
                        render: (_, row) => (
                          <Space>
                            <Typography.Link onClick={() => setDetail(row)}>详情</Typography.Link>
                            {!row.read ? (
                              <Typography.Link onClick={() => void onMarkRead(row.id)}>已读</Typography.Link>
                            ) : null}
                          </Space>
                        ),
                      },
                    ]}
                  />
                </SettingSection>
              </>
            ),
          },
          {
            key: 'templates',
            label: '通知模板',
            children: (
              <SettingSection
                title="模板列表"
                description="标题/正文支持 ${name} 占位；发送时可选用模板"
                extra={
                  <Button type="primary" size="small" onClick={openCreateTemplate}>
                    新建
                  </Button>
                }
              >
                <Table<NotifyTemplate>
                  rowKey="id"
                  dataSource={templates}
                  pagination={false}
                  locale={{ emptyText: <EmptyState description="暂无模板" /> }}
                  columns={[
                    { title: '名称', dataIndex: 'name', width: 160 },
                    { title: '编码', dataIndex: 'code', width: 140 },
                    {
                      title: '渠道',
                      dataIndex: 'channel',
                      width: 100,
                      render: (v: string) =>
                        CHANNEL_OPTIONS.find((c) => c.value === v)?.label ?? v,
                    },
                    {
                      title: '状态',
                      dataIndex: 'status',
                      width: 100,
                      render: (v: string) => <StatusBadge code={v} map={ENABLED_STATUS} />,
                    },
                    {
                      title: '标题模板',
                      dataIndex: 'titleTemplate',
                      ellipsis: true,
                    },
                    {
                      title: '操作',
                      width: 140,
                      render: (_, row) => (
                        <Space>
                          <Typography.Link onClick={() => openEditTemplate(row)}>编辑</Typography.Link>
                          <Popconfirm title="确认删除该模板？" onConfirm={() => void onDeleteTemplate(row.id)}>
                            <Typography.Link type="danger">删除</Typography.Link>
                          </Popconfirm>
                        </Space>
                      ),
                    },
                  ]}
                />
              </SettingSection>
            ),
          },
          {
            key: 'logs',
            label: '发送历史',
            children: (
              <SettingSection
                title="发送流水"
                description="含直发与模板发送；失败会记录错误信息"
                extra={
                  <Button size="small" onClick={() => void fetchLogs()}>
                    刷新
                  </Button>
                }
              >
                <Table<NotifySendLog>
                  rowKey="id"
                  dataSource={logs}
                  pagination={false}
                  locale={{ emptyText: <EmptyState description="暂无发送记录" /> }}
                  columns={[
                    {
                      title: '状态',
                      dataIndex: 'status',
                      width: 90,
                      render: (v: string) => <StatusBadge code={v || 'FAILED'} map={SEND_STATUS} />,
                    },
                    {
                      title: '渠道',
                      dataIndex: 'channel',
                      width: 90,
                      render: (v: string) =>
                        CHANNEL_OPTIONS.find((c) => c.value === v)?.label ?? v,
                    },
                    { title: '接收人', dataIndex: 'toKey', width: 140, ellipsis: true },
                    { title: '标题', dataIndex: 'title', ellipsis: true },
                    { title: '时间', dataIndex: 'createTime', width: 180, render: (v?: string) => v || '—' },
                    {
                      title: '操作',
                      width: 80,
                      render: (_, row) => (
                        <Typography.Link onClick={() => setLogDetail(row)}>详情</Typography.Link>
                      ),
                    },
                  ]}
                />
              </SettingSection>
            ),
          },
        ]}
      />

      <FormDrawer
        title="发送通知"
        open={sendOpen}
        form={form}
        saveText="发送"
        onSave={() => void onSend()}
        onClose={() => setSendOpen(false)}
        extra={
          <Space>
            <Button onClick={() => setSendOpen(false)}>取消</Button>
            <Button type="primary" loading={submitting} onClick={() => void onSend()}>
              发送
            </Button>
          </Space>
        }
      >
        <FormItem name="mode" label="方式" rules={[{ required: true }]}>
          <Select
            options={[
              { value: 'direct', label: '直接填写' },
              { value: 'template', label: '使用模板' },
            ]}
          />
        </FormItem>
        <FormItem name="to" label="接收人" rules={[{ required: true, message: '必填' }]}>
          <Input placeholder="用户标识 / 邮箱 / 手机号" />
        </FormItem>
        {sendMode === 'template' ? (
          <>
            <FormItem name="templateCode" label="模板" rules={[{ required: true, message: '请选择模板' }]}>
              <Select
                options={enabledTemplates.map((t) => ({
                  value: t.code,
                  label: `${t.name} (${t.code}) · ${t.channel}`,
                }))}
                placeholder={enabledTemplates.length ? '选择模板' : '暂无启用模板'}
              />
            </FormItem>
            <FormItem
              name="varsJson"
              label="变量"
              full
              extra='可选，JSON 对象，对应模板中的 ${key}'
            >
              <Input.TextArea rows={3} placeholder='{"name":"张三"}' />
            </FormItem>
          </>
        ) : (
          <>
            <FormItem name="channel" label="渠道" rules={[{ required: true }]}>
              <Select options={CHANNEL_OPTIONS} />
            </FormItem>
            <FormItem name="title" label="标题" rules={[{ required: true }]}>
              <Input />
            </FormItem>
            <FormItem name="content" label="内容" rules={[{ required: true }]} full>
              <Input.TextArea rows={4} />
            </FormItem>
          </>
        )}
        {sendMode === 'template' && selectedTemplateCode ? (
          <Typography.Paragraph type="secondary" style={{ marginTop: 8 }}>
            {enabledTemplates.find((t) => t.code === selectedTemplateCode)?.titleTemplate}
          </Typography.Paragraph>
        ) : null}
      </FormDrawer>

      <FormModal
        title={editingTemplate ? '编辑模板' : '新建模板'}
        open={templateOpen}
        form={templateForm}
        confirmLoading={submitting}
        onOk={() => void onSaveTemplate()}
        onCancel={() => setTemplateOpen(false)}
      >
        <FormItem name="code" label="编码" rules={[{ required: true, message: '必填' }]}>
          <Input placeholder="如：welcome_inbox" disabled={!!editingTemplate} />
        </FormItem>
        <FormItem name="name" label="名称" rules={[{ required: true }]}>
          <Input />
        </FormItem>
        <FormItem name="channel" label="渠道" rules={[{ required: true }]}>
          <Select options={CHANNEL_OPTIONS} />
        </FormItem>
        <FormItem name="status" label="状态" rules={[{ required: true }]}>
          <Select
            options={[
              { value: 'ENABLED', label: '启用' },
              { value: 'DISABLED', label: '停用' },
            ]}
          />
        </FormItem>
        <FormItem
          name="titleTemplate"
          label="标题模板"
          rules={[{ required: true }]}
          extra="支持 ${name} 占位"
        >
          <Input />
        </FormItem>
        <FormItem name="contentTemplate" label="正文模板" rules={[{ required: true }]} full>
          <Input.TextArea rows={4} />
        </FormItem>
      </FormModal>

      <DetailDrawer title={detail?.title || '通知详情'} open={!!detail} onClose={() => setDetail(null)}>
        {detail ? (
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <StatusBadge code={detail.read ? 'READ' : 'UNREAD'} map={READ_STATUS} />
            <Typography.Text type="secondary">{detail.createTime || '—'}</Typography.Text>
            <Typography.Paragraph style={{ whiteSpace: 'pre-wrap' }}>
              {detail.content || '无内容'}
            </Typography.Paragraph>
            {!detail.read ? (
              <Button type="primary" onClick={() => void onMarkRead(detail.id).then(() => setDetail(null))}>
                标记已读
              </Button>
            ) : null}
          </Space>
        ) : null}
      </DetailDrawer>

      <DetailDrawer title={logDetail?.title || '发送详情'} open={!!logDetail} onClose={() => setLogDetail(null)}>
        {logDetail ? (
          <Space direction="vertical" size={8} style={{ width: '100%' }}>
            <StatusBadge code={logDetail.status || 'FAILED'} map={SEND_STATUS} />
            <Typography.Text>渠道：{logDetail.channel}</Typography.Text>
            <Typography.Text>接收人：{logDetail.toKey}</Typography.Text>
            <Typography.Text type="secondary">{logDetail.createTime || '—'}</Typography.Text>
            <Typography.Paragraph style={{ whiteSpace: 'pre-wrap' }}>
              {logDetail.content || '无内容'}
            </Typography.Paragraph>
            {logDetail.error ? (
              <Typography.Text type="danger">错误：{logDetail.error}</Typography.Text>
            ) : null}
          </Space>
        ) : null}
      </DetailDrawer>
    </div>
  );
}
