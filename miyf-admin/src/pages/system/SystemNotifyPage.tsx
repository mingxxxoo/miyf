import { useCallback, useEffect, useState } from 'react';
import { Button, Form, Input, Modal, Select, Space, Table, Tag, message } from 'antd';
import {
  sysNotificationApi,
  type InboxItem,
} from '@/pages/system/api';

/**
 * 系统通知：发送 + 站内信查询。
 */
export default function SystemNotifyPage() {
  const [loading, setLoading] = useState(false);
  const [items, setItems] = useState<InboxItem[]>([]);
  const [userKey, setUserKey] = useState('admin');
  const [sendOpen, setSendOpen] = useState(false);
  const [form] = Form.useForm();

  const fetchInbox = useCallback(async () => {
    if (!userKey.trim()) {
      message.warning('请输入 userKey');
      return;
    }
    setLoading(true);
    try {
      const list = await sysNotificationApi.listInbox(userKey.trim());
      setItems(list);
    } catch (err) {
      setItems([]);
      message.error(err instanceof Error ? err.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, [userKey]);

  useEffect(() => {
    void fetchInbox();
  }, [fetchInbox]);

  const onSend = async () => {
    const values = await form.validateFields();
    try {
      await sysNotificationApi.send({
        channel: values.channel,
        to: values.to,
        title: values.title,
        content: values.content,
      });
      message.success('已发送');
      setSendOpen(false);
      form.resetFields();
      if (values.channel === 'INBOX' && values.to) {
        setUserKey(values.to);
      }
      await fetchInbox();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '发送失败');
    }
  };

  const onMarkRead = async (id: string) => {
    try {
      await sysNotificationApi.markRead(id, userKey.trim());
      message.success('已标记已读');
      await fetchInbox();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    }
  };

  return (
    <div className="ck-page">
      <h2 className="ck-page-title">通知中心</h2>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input
          style={{ width: 220 }}
          value={userKey}
          onChange={(e) => setUserKey(e.target.value)}
          placeholder="userKey"
          allowClear
        />
        <Button onClick={() => void fetchInbox()}>查询站内信</Button>
        <Button
          type="primary"
          onClick={() => {
            form.setFieldsValue({ channel: 'INBOX', to: userKey });
            setSendOpen(true);
          }}
        >
          发送通知
        </Button>
      </Space>
      <Table
        rowKey="id"
        loading={loading}
        dataSource={items}
        pagination={{ pageSize: 10 }}
        columns={[
          { title: '标题', dataIndex: 'title', ellipsis: true },
          { title: '内容', dataIndex: 'content', ellipsis: true },
          {
            title: '状态',
            dataIndex: 'read',
            width: 90,
            render: (v: boolean) => (v ? <Tag>已读</Tag> : <Tag color="blue">未读</Tag>),
          },
          { title: '时间', dataIndex: 'createdAt', width: 200 },
          {
            title: '操作',
            width: 100,
            render: (_, row) =>
              row.read ? null : (
                <Button type="link" onClick={() => void onMarkRead(row.id)}>
                  已读
                </Button>
              ),
          },
        ]}
      />
      <Modal
        title="发送通知"
        open={sendOpen}
        onCancel={() => setSendOpen(false)}
        onOk={() => void onSend()}
        destroyOnClose
      >
        <Form form={form} layout="vertical" initialValues={{ channel: 'INBOX' }}>
          <Form.Item name="channel" label="渠道" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'INBOX', label: '站内信' },
                { value: 'EMAIL', label: '邮件' },
                { value: 'SMS', label: '短信' },
              ]}
            />
          </Form.Item>
          <Form.Item name="to" label="接收人" rules={[{ required: true, message: '必填' }]}>
            <Input placeholder="用户标识 / 邮箱 / 手机号" />
          </Form.Item>
          <Form.Item name="title" label="标题" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="content" label="内容" rules={[{ required: true }]}>
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
