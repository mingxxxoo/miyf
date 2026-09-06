import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Tag,
  message,
} from 'antd';
import PageTable from '@/components/PageTable';
import { sysConfigApi, type SysConfig } from '@/pages/system/api';

const VALUE_TYPES = [
  { value: 'STRING', label: '字符串' },
  { value: 'NUMBER', label: '数字' },
  { value: 'BOOLEAN', label: '布尔' },
  { value: 'JSON', label: 'JSON' },
];

/**
 * 系统配置 CRUD。
 */
export default function SystemConfigPage() {
  const [loading, setLoading] = useState(false);
  const [all, setAll] = useState<SysConfig[]>([]);
  const [keyword, setKeyword] = useState('');
  const [groupCode, setGroupCode] = useState<string | undefined>();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<SysConfig | null>(null);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const list = await sysConfigApi.list({
        groupCode: groupCode || undefined,
        keyword: keyword.trim() || undefined,
      });
      setAll(list);
    } catch (err) {
      setAll([]);
      message.error(err instanceof Error ? err.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, [groupCode, keyword]);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const groupOptions = useMemo(() => {
    const set = new Set(all.map((c) => c.groupCode).filter(Boolean));
    return Array.from(set)
      .sort()
      .map((g) => ({ value: g, label: g }));
  }, [all]);

  const data = useMemo(() => {
    const start = (page - 1) * pageSize;
    return all.slice(start, start + pageSize);
  }, [all, page, pageSize]);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({
      valueType: 'STRING',
      groupCode: 'default',
      status: 'ENABLED',
      sortOrder: 0,
    });
    setOpen(true);
  };

  const openEdit = (row: SysConfig) => {
    setEditing(row);
    form.setFieldsValue({
      configKey: row.configKey,
      configValue: row.configValue,
      valueType: row.valueType,
      groupCode: row.groupCode,
      name: row.name,
      description: row.description,
      status: row.status,
      sortOrder: row.sortOrder ?? 0,
    });
    setOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    const payload = {
      configKey: values.configKey as string,
      configValue: values.configValue as string | undefined,
      valueType: values.valueType as string,
      groupCode: values.groupCode as string,
      name: values.name as string,
      description: values.description as string | undefined,
      status: values.status as string,
      sortOrder: values.sortOrder as number | undefined,
    };
    try {
      if (editing) {
        await sysConfigApi.update(editing.id, payload);
        message.success('配置已更新');
      } else {
        await sysConfigApi.create(payload);
        message.success('配置已创建');
      }
      setOpen(false);
      void fetchData();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await sysConfigApi.remove(id);
      message.success('已删除');
      void fetchData();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '删除失败');
    }
  };

  return (
    <div className="ck-page">
      <h2 className="ck-page-title">系统配置</h2>
      <PageTable<SysConfig>
        title="配置列表"
        loading={loading}
        rowKey="id"
        extra={
          <Space wrap>
            <Input.Search
              allowClear
              placeholder="键 / 名称"
              style={{ width: 200 }}
              onSearch={(v) => {
                setPage(1);
                setKeyword(v);
              }}
            />
            <Select
              allowClear
              placeholder="分组"
              style={{ width: 140 }}
              options={groupOptions}
              value={groupCode}
              onChange={(v) => {
                setPage(1);
                setGroupCode(v);
              }}
            />
            <Button type="primary" onClick={openCreate}>
              新增配置
            </Button>
          </Space>
        }
        columns={[
          { title: '分组', dataIndex: 'groupCode', width: 100 },
          { title: '键', dataIndex: 'configKey', width: 200 },
          { title: '名称', dataIndex: 'name', width: 140 },
          {
            title: '值',
            dataIndex: 'configValue',
            ellipsis: true,
            render: (v?: string) => v || '—',
          },
          {
            title: '类型',
            dataIndex: 'valueType',
            width: 90,
            render: (v: string) => VALUE_TYPES.find((t) => t.value === v)?.label || v,
          },
          {
            title: '状态',
            dataIndex: 'status',
            width: 90,
            render: (v: string) => (
              <Tag color={v === 'ENABLED' ? 'success' : 'default'}>
                {v === 'ENABLED' ? '启用' : '禁用'}
              </Tag>
            ),
          },
          { title: '排序', dataIndex: 'sortOrder', width: 70 },
          {
            title: '操作',
            width: 150,
            render: (_, row) => (
              <Space>
                <Button type="link" size="small" onClick={() => openEdit(row)}>
                  编辑
                </Button>
                <Popconfirm title="确认删除该配置？" onConfirm={() => void handleDelete(row.id)}>
                  <Button type="link" size="small" danger>
                    删除
                  </Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
        dataSource={data}
        pagination={{
          current: page,
          pageSize,
          total: all.length,
          onChange: (p, ps) => {
            setPage(p);
            setPageSize(ps);
          },
        }}
        locale={{ emptyText: '暂无配置' }}
      />

      <Modal
        title={editing ? '编辑配置' : '新增配置'}
        open={open}
        width={560}
        onOk={() => void handleSubmit()}
        onCancel={() => setOpen(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="configKey" label="配置键" rules={[{ required: true }]}>
            <Input placeholder="如：site.name" disabled={!!editing} />
          </Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="configValue" label="配置值">
            <Input.TextArea rows={3} placeholder="字符串 / 数字 / true|false / JSON" />
          </Form.Item>
          <Form.Item name="valueType" label="值类型" rules={[{ required: true }]}>
            <Select options={VALUE_TYPES} />
          </Form.Item>
          <Form.Item name="groupCode" label="分组" rules={[{ required: true }]}>
            <Input placeholder="如：site / security" />
          </Form.Item>
          <Form.Item name="status" label="状态" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'ENABLED', label: '启用' },
                { value: 'DISABLED', label: '禁用' },
              ]}
            />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
