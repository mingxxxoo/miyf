import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Tag, message } from 'antd';
import PageTable from '@/components/PageTable';
import { iamOrgUnitApi, type IamOrgUnit } from '@/modules/iam/api';

export default function OrgUnitsPage() {
  const [loading, setLoading] = useState(false);
  const [all, setAll] = useState<IamOrgUnit[]>([]);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<IamOrgUnit | null>(null);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      setAll(await iamOrgUnitApi.list());
    } catch {
      setAll([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const data = useMemo(() => {
    const start = (page - 1) * pageSize;
    return all.slice(start, start + pageSize);
  }, [all, page, pageSize]);

  const parentOptions = useMemo(
    () =>
      all
        .filter((u) => !editing || u.id !== editing.id)
        .map((u) => ({ value: u.id, label: `${u.name} (${u.code})` })),
    [all, editing],
  );

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ status: 'ENABLED', sortOrder: 0 });
    setOpen(true);
  };

  const openEdit = (row: IamOrgUnit) => {
    setEditing(row);
    form.setFieldsValue({
      code: row.code,
      name: row.name,
      parentId: row.parentId,
      sortOrder: row.sortOrder ?? 0,
      status: row.status,
    });
    setOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    try {
      const payload = {
        code: values.code as string,
        name: values.name as string,
        parentId: values.parentId as string | undefined,
        sortOrder: values.sortOrder as number | undefined,
        status: values.status as string,
      };
      if (editing) {
        await iamOrgUnitApi.update(editing.id, payload);
        message.success('单位已更新');
      } else {
        await iamOrgUnitApi.create(payload);
        message.success('单位已创建');
      }
      setOpen(false);
      void fetchData();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await iamOrgUnitApi.remove(id);
      message.success('已删除');
      void fetchData();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '删除失败');
    }
  };

  return (
    <div className="ck-page">
      <h2 className="ck-page-title">单位管理</h2>
      <PageTable<IamOrgUnit>
        title="组织单位"
        loading={loading}
        rowKey="id"
        extra={
          <Button type="primary" onClick={openCreate}>
            新增单位
          </Button>
        }
        columns={[
          { title: '编码', dataIndex: 'code' },
          { title: '名称', dataIndex: 'name' },
          {
            title: '上级',
            dataIndex: 'parentId',
            render: (v?: string) => {
              if (!v) return '—';
              const p = all.find((u) => u.id === v);
              return p?.name || v;
            },
          },
          { title: '排序', dataIndex: 'sortOrder', width: 80 },
          {
            title: '状态',
            dataIndex: 'status',
            render: (v?: string) =>
              v === 'DISABLED' ? <Tag>停用</Tag> : <Tag color="success">启用</Tag>,
          },
          {
            title: '操作',
            width: 160,
            render: (_, row) => (
              <Space>
                <Button type="link" size="small" onClick={() => openEdit(row)}>
                  编辑
                </Button>
                <Popconfirm title="确认删除该单位？" onConfirm={() => void handleDelete(row.id)}>
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
        locale={{ emptyText: '暂无组织单位' }}
      />

      <Modal
        title={editing ? '编辑单位' : '新增单位'}
        open={open}
        onOk={() => void handleSubmit()}
        onCancel={() => setOpen(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="code" label="编码" rules={[{ required: true }]}>
            <Input placeholder="如：HQ" />
          </Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input placeholder="如：总部" />
          </Form.Item>
          <Form.Item name="parentId" label="上级单位">
            <Select allowClear options={parentOptions} placeholder="无则留空" />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="status" label="状态" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'ENABLED', label: '启用' },
                { value: 'DISABLED', label: '停用' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
