import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Form, Input, Modal, Select, Space, Tag, message, Popconfirm } from 'antd';
import PageTable from '@/components/PageTable';
import {
  iamOrgUnitApi,
  iamRoleApi,
  iamUserApi,
  type IamOrgUnit,
  type IamRole,
  type IamUser,
} from '@/modules/iam/api';

export default function AdminsPage() {
  const [loading, setLoading] = useState(false);
  const [all, setAll] = useState<IamUser[]>([]);
  const [orgs, setOrgs] = useState<IamOrgUnit[]>([]);
  const [roles, setRoles] = useState<IamRole[]>([]);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<IamUser | null>(null);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [users, orgList, roleList] = await Promise.all([
        iamUserApi.list(),
        iamOrgUnitApi.list(),
        iamRoleApi.list(),
      ]);
      setAll(users);
      setOrgs(orgList);
      setRoles(roleList);
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

  const orgName = (id?: string) => {
    if (!id) return '—';
    return orgs.find((o) => o.id === id)?.name || id;
  };

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ status: 'ENABLED' });
    setOpen(true);
  };

  const openEdit = (row: IamUser) => {
    setEditing(row);
    form.setFieldsValue({
      username: row.username,
      nickname: row.nickname,
      status: row.status,
      orgUnitId: row.orgUnitId,
      password: undefined,
      roleIds: undefined,
    });
    setOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    try {
      const payload = {
        username: values.username as string,
        nickname: values.nickname as string | undefined,
        password: values.password as string | undefined,
        status: (values.status as string) ?? 'ENABLED',
        orgUnitId: values.orgUnitId as string | undefined,
        roleIds: values.roleIds as string[] | undefined,
      };
      if (editing) {
        await iamUserApi.update(editing.id, {
          username: payload.username,
          nickname: payload.nickname,
          password: payload.password,
          status: payload.status,
          orgUnitId: payload.orgUnitId,
          roleIds: payload.roleIds,
        });
        message.success('人员已更新');
      } else {
        if (!payload.password) {
          message.error('请填写初始密码');
          return;
        }
        await iamUserApi.create({
          username: payload.username,
          nickname: payload.nickname,
          password: payload.password,
          status: payload.status,
          orgUnitId: payload.orgUnitId,
          roleIds: payload.roleIds,
        });
        message.success('人员已创建');
      }
      setOpen(false);
      form.resetFields();
      void fetchData();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await iamUserApi.remove(id);
      message.success('已删除');
      void fetchData();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '删除失败');
    }
  };

  return (
    <div className="ck-page">
      <h2 className="ck-page-title">人员管理</h2>
      <PageTable<IamUser>
        title="系统用户"
        loading={loading}
        rowKey="id"
        extra={
          <Button type="primary" onClick={openCreate}>
            新增人员
          </Button>
        }
        columns={[
          { title: '用户名', dataIndex: 'username' },
          { title: '昵称', dataIndex: 'nickname', render: (v?: string) => v || '—' },
          {
            title: '所属单位',
            dataIndex: 'orgUnitId',
            render: (v?: string) => orgName(v),
          },
          {
            title: '状态',
            dataIndex: 'status',
            render: (v?: string) =>
              v === 'DISABLED' ? <Tag>停用</Tag> : <Tag color="success">启用</Tag>,
          },
          { title: '创建时间', dataIndex: 'createdAt', render: (v?: string) => v || '—' },
          {
            title: '操作',
            width: 160,
            render: (_, row) => (
              <Space>
                <Button type="link" size="small" onClick={() => openEdit(row)}>
                  编辑
                </Button>
                <Popconfirm title="确认删除该人员？" onConfirm={() => void handleDelete(row.id)}>
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
        locale={{ emptyText: '暂无系统用户' }}
      />

      <Modal
        title={editing ? '编辑人员' : '新增人员'}
        open={open}
        onOk={() => void handleSubmit()}
        onCancel={() => setOpen(false)}
      >
        <Form form={form} layout="vertical" initialValues={{ status: 'ENABLED' }}>
          <Form.Item name="username" label="用户名" rules={[{ required: true }]}>
            <Input disabled={!!editing} />
          </Form.Item>
          <Form.Item name="nickname" label="昵称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item
            name="password"
            label={editing ? '新密码（留空不改）' : '初始密码'}
            rules={editing ? [] : [{ required: true }]}
          >
            <Input.Password />
          </Form.Item>
          <Form.Item name="orgUnitId" label="所属单位">
            <Select
              allowClear
              options={orgs.map((o) => ({ value: o.id, label: `${o.name} (${o.code})` }))}
            />
          </Form.Item>
          <Form.Item name="roleIds" label="角色">
            <Select
              mode="multiple"
              allowClear
              options={roles.map((r) => ({ value: r.id, label: `${r.name} (${r.code})` }))}
            />
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
