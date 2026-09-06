import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Form, Input, Modal, Popconfirm, Select, Space, message } from 'antd';
import PageTable from '@/components/PageTable';
import { iamPermGroupApi, iamRoleApi, type IamPermGroup, type IamRole } from '@/modules/iam/api';

export default function RolesPage() {
  const [loading, setLoading] = useState(false);
  const [all, setAll] = useState<IamRole[]>([]);
  const [groups, setGroups] = useState<IamPermGroup[]>([]);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<IamRole | null>(null);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [roles, groupList] = await Promise.all([iamRoleApi.list(), iamPermGroupApi.list()]);
      setAll(roles);
      setGroups(groupList);
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

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    setOpen(true);
  };

  const openEdit = (row: IamRole) => {
    setEditing(row);
    form.setFieldsValue({
      code: row.code,
      name: row.name,
      description: row.description,
      groupIds: undefined,
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
        groupIds: values.groupIds as string[] | undefined,
      };
      if (editing) {
        await iamRoleApi.update(editing.id, payload);
        message.success('角色已更新');
      } else {
        await iamRoleApi.create(payload);
        message.success('角色已创建');
      }
      setOpen(false);
      void fetchData();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await iamRoleApi.remove(id);
      message.success('已删除');
      void fetchData();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '删除失败');
    }
  };

  return (
    <div className="ck-page">
      <h2 className="ck-page-title">角色管理</h2>
      <PageTable<IamRole>
        title="角色列表"
        loading={loading}
        rowKey="id"
        extra={
          <Button type="primary" onClick={openCreate}>
            新增角色
          </Button>
        }
        columns={[
          { title: '编码', dataIndex: 'code' },
          { title: '名称', dataIndex: 'name' },
          { title: '说明', dataIndex: 'description', render: (v?: string) => v || '—' },
          { title: '创建时间', dataIndex: 'createdAt', render: (v?: string) => v || '—' },
          {
            title: '操作',
            width: 160,
            render: (_, row) => (
              <Space>
                <Button type="link" size="small" onClick={() => openEdit(row)}>
                  编辑
                </Button>
                <Popconfirm title="确认删除该角色？" onConfirm={() => void handleDelete(row.id)}>
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
        locale={{ emptyText: '暂无角色配置' }}
      />
      <Modal
        title={editing ? '编辑角色' : '新增角色'}
        open={open}
        onOk={() => void handleSubmit()}
        onCancel={() => setOpen(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="code" label="编码" rules={[{ required: true }]}>
            <Input placeholder="如：KITCHEN_MANAGER" disabled={!!editing} />
          </Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input placeholder="如：厨房管家" />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="groupIds" label="权限组">
            <Select
              mode="multiple"
              allowClear
              options={groups.map((g) => ({ value: g.id, label: `${g.name} (${g.code})` }))}
              placeholder="绑定权限组"
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
