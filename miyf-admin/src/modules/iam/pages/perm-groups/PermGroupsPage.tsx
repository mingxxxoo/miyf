import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, message } from 'antd';
import PageTable from '@/components/PageTable';
import {
  iamPermGroupApi,
  iamPermissionApi,
  type IamPermGroup,
  type IamPermission,
} from '@/modules/iam/api';

export default function PermGroupsPage() {
  const [loading, setLoading] = useState(false);
  const [all, setAll] = useState<IamPermGroup[]>([]);
  const [permissions, setPermissions] = useState<IamPermission[]>([]);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<IamPermGroup | null>(null);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [groups, perms] = await Promise.all([iamPermGroupApi.list(), iamPermissionApi.list()]);
      setAll(groups);
      setPermissions(perms);
    } catch {
      setAll([]);
      setPermissions([]);
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

  const permOptions = useMemo(
    () =>
      permissions.map((p) => ({
        value: p.id,
        label: `${p.code} · ${p.name}`,
      })),
    [permissions],
  );

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ sortOrder: 0, permissionIds: [] });
    setOpen(true);
  };

  const openEdit = (row: IamPermGroup) => {
    setEditing(row);
    const related = permissions.filter((p) => p.groupCode === row.code).map((p) => p.id);
    form.setFieldsValue({
      code: row.code,
      name: row.name,
      description: row.description,
      sortOrder: row.sortOrder ?? 0,
      permissionIds: related,
    });
    setOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    try {
      const payload = {
        code: values.code as string,
        name: values.name as string,
        description: values.description as string | undefined,
        sortOrder: values.sortOrder as number | undefined,
        permissionIds: (values.permissionIds as string[] | undefined) ?? [],
      };
      if (editing) {
        await iamPermGroupApi.update(editing.id, payload);
        message.success('权限组已更新');
      } else {
        await iamPermGroupApi.create(payload);
        message.success('权限组已创建');
      }
      setOpen(false);
      void fetchData();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await iamPermGroupApi.remove(id);
      message.success('已删除');
      void fetchData();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '删除失败');
    }
  };

  return (
    <div className="ck-page">
      <h2 className="ck-page-title">权限组</h2>
      <PageTable<IamPermGroup>
        title="权限组列表"
        loading={loading}
        rowKey="id"
        extra={
          <Button type="primary" onClick={openCreate}>
            新增权限组
          </Button>
        }
        columns={[
          { title: '编码', dataIndex: 'code' },
          { title: '名称', dataIndex: 'name' },
          { title: '说明', dataIndex: 'description', render: (v?: string) => v || '—' },
          { title: '排序', dataIndex: 'sortOrder', width: 80 },
          {
            title: '操作',
            width: 160,
            render: (_, row) => (
              <Space>
                <Button type="link" size="small" onClick={() => openEdit(row)}>
                  编辑
                </Button>
                <Popconfirm title="确认删除该权限组？" onConfirm={() => void handleDelete(row.id)}>
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
        locale={{ emptyText: '暂无权限组' }}
      />

      <Modal
        title={editing ? '编辑权限组' : '新增权限组'}
        open={open}
        width={560}
        onOk={() => void handleSubmit()}
        onCancel={() => setOpen(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="code" label="编码" rules={[{ required: true }]}>
            <Input placeholder="如：iam_user" disabled={!!editing} />
          </Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="permissionIds" label="包含权限">
            <Select
              mode="multiple"
              allowClear
              optionFilterProp="label"
              options={permOptions}
              placeholder="选择权限"
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
