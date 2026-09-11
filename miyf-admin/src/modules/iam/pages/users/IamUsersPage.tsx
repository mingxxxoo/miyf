import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Form, Input, Popconfirm, Select, Space, message } from 'antd';
import PageTable from '@/components/PageTable';
import {
  EmptyState,
  FormItem,
  FormModal,
  PageHeader,
  PageToolbar,
  StatusBadge,
} from '@/ui';
import {
  iamOrgUnitApi,
  iamRoleApi,
  iamUserApi,
  type IamOrgUnit,
  type IamRole,
  type IamUser,
} from '@/modules/iam/api';
import { useClientPager } from '@/hooks/useClientPager';
import { useSubmitting } from '@/hooks/useSubmitting';
import { USER_STATUS } from '@/constants/status';
import { notifyError } from '@/api/errors';
import { usePermissionStore } from '@/stores/permissionStore';

/**
 * 系统用户：维护登录账号、组织、角色与状态（客户端分页）。
 */
export default function IamUsersPage() {
  const canUpdate = usePermissionStore((s) => s.hasPermission('iam:user:update'));
  const canCreate = usePermissionStore((s) => s.hasPermission('iam:user:create'));
  const canDelete = usePermissionStore((s) => s.hasPermission('iam:user:delete'));
  const [loading, setLoading] = useState(false);
  const [all, setAll] = useState<IamUser[]>([]);
  const [orgs, setOrgs] = useState<IamOrgUnit[]>([]);
  const [roles, setRoles] = useState<IamRole[]>([]);
  const [keyword, setKeyword] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<IamUser | null>(null);
  const [form] = Form.useForm();
  const { submitting, run } = useSubmitting();

  const orgMap = useMemo(() => new Map(orgs.map((o) => [o.id, o])), [orgs]);
  const roleMap = useMemo(() => new Map(roles.map((r) => [r.id, r])), [roles]);

  const filtered = useMemo(() => {
    const q = keyword.trim().toLowerCase();
    if (!q) return all;
    return all.filter((u) => {
      const orgName = u.orgUnitId ? orgMap.get(u.orgUnitId)?.name ?? '' : '';
      return (
        u.username.toLowerCase().includes(q) ||
        (u.nickname || '').toLowerCase().includes(q) ||
        orgName.toLowerCase().includes(q)
      );
    });
  }, [all, keyword, orgMap]);

  const { data, pagination, setPage } = useClientPager(filtered);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [userList, orgList, roleList] = await Promise.all([
        iamUserApi.list(),
        iamOrgUnitApi.list(),
        iamRoleApi.list(),
      ]);
      setAll(userList);
      setOrgs(orgList);
      setRoles(roleList);
    } catch (err) {
      setAll([]);
      setOrgs([]);
      setRoles([]);
      notifyError(err, '加载用户列表失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const orgOptions = useMemo(
    () => orgs.map((o) => ({ value: o.id, label: `${o.name} (${o.code})` })),
    [orgs],
  );

  const roleOptions = useMemo(
    () => roles.map((r) => ({ value: r.id, label: `${r.name} (${r.code})` })),
    [roles],
  );

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ status: 'ENABLED', roleIds: [] });
    setOpen(true);
  };

  const openEdit = (row: IamUser) => {
    setEditing(row);
    form.setFieldsValue({
      username: row.username,
      nickname: row.nickname,
      status: row.status,
      orgUnitId: row.orgUnitId,
      roleIds: row.roleIds ?? [],
      password: undefined,
    });
    setOpen(true);
  };

  const handleSubmit = () =>
    void run(async () => {
      const values = await form.validateFields();
      const payload = {
        username: values.username as string,
        password: values.password as string | undefined,
        nickname: (values.nickname as string | undefined) || undefined,
        status: values.status as string,
        orgUnitId: values.orgUnitId as string | undefined,
        roleIds: (values.roleIds as string[] | undefined) ?? [],
      };
      try {
        if (editing) {
          await iamUserApi.update(editing.id, payload);
          message.success('用户已更新');
        } else {
          if (!payload.password) {
            message.error('请设置登录密码');
            return;
          }
          await iamUserApi.create({
            ...payload,
            password: payload.password,
          });
          message.success('用户已创建');
        }
        setOpen(false);
        void fetchData();
      } catch (err) {
        notifyError(err, '保存失败');
      }
    });

  const handleDelete = (id: string) =>
    void run(async () => {
      try {
        await iamUserApi.remove(id);
        message.success('已删除');
        void fetchData();
      } catch (err) {
        notifyError(err, '删除失败');
      }
    });

  const toggleStatus = (row: IamUser) =>
    void run(async () => {
      if (!canUpdate) return;
      const next = row.status === 'DISABLED' ? 'ENABLED' : 'DISABLED';
      try {
        await iamUserApi.updateStatus(row.id, next);
        message.success(next === 'DISABLED' ? '已禁用登录' : '已恢复登录');
        void fetchData();
      } catch (err) {
        notifyError(err, '更新状态失败');
      }
    });

  return (
    <div className="ck-page">
      <PageHeader title="用户管理" description="维护系统登录账号、所属组织、角色与状态；可删除账号" />
      <PageToolbar
        left={
          <Input.Search
            allowClear
            placeholder="搜索用户名 / 昵称 / 组织"
            style={{ width: 260 }}
            onSearch={(v) => {
              setPage(1);
              setKeyword(v);
            }}
            onChange={(e) => {
              if (!e.target.value) {
                setPage(1);
                setKeyword('');
              }
            }}
          />
        }
        right={
          canCreate ? (
            <Button type="primary" onClick={openCreate}>
              新增用户
            </Button>
          ) : null
        }
      />

      <PageTable<IamUser>
        title="系统用户"
        loading={loading}
        rowKey="id"
        columns={[
          { title: '用户名', dataIndex: 'username' },
          {
            title: '昵称',
            dataIndex: 'nickname',
            render: (v?: string) => v || '—',
          },
          {
            title: '所属组织',
            dataIndex: 'orgUnitId',
            render: (v?: string) => {
              if (!v) return '—';
              return orgMap.get(v)?.name || v;
            },
          },
          {
            title: '角色',
            dataIndex: 'roleIds',
            render: (ids?: string[]) => {
              if (!ids?.length) return '—';
              return ids.map((id) => roleMap.get(id)?.name || id).join('、');
            },
          },
          {
            title: '状态',
            dataIndex: 'status',
            width: 90,
            render: (v?: string) => <StatusBadge code={v || 'ENABLED'} map={USER_STATUS} />,
          },
          {
            title: '操作',
            width: 240,
            render: (_, row) => {
              const disabled = row.status === 'DISABLED';
              return (
                <Space>
                  {canUpdate ? (
                    <Button type="link" size="small" onClick={() => openEdit(row)}>
                      编辑
                    </Button>
                  ) : null}
                  {canUpdate ? (
                    <Popconfirm
                      title={disabled ? '确认恢复该账号登录？' : '确认禁用该账号登录？'}
                      description={
                        disabled
                          ? '解禁后可再次登录管理端'
                          : '禁用后无法登录，已发令牌立即失效'
                      }
                      onConfirm={() => toggleStatus(row)}
                    >
                      <Button type="link" size="small" danger={!disabled}>
                        {disabled ? '解禁' : '禁用'}
                      </Button>
                    </Popconfirm>
                  ) : null}
                  {canDelete ? (
                    <Popconfirm title="确认删除该用户？删除后不可恢复" onConfirm={() => handleDelete(row.id)}>
                      <Button type="link" size="small" danger>
                        删除
                      </Button>
                    </Popconfirm>
                  ) : null}
                </Space>
              );
            },
          },
        ]}
        dataSource={data}
        pagination={pagination}
        locale={{ emptyText: <EmptyState description="暂无系统用户" /> }}
      />

      <FormModal
        title={editing ? '编辑用户' : '新增用户'}
        open={open}
        form={form}
        confirmLoading={submitting}
        onOk={handleSubmit}
        onCancel={() => setOpen(false)}
      >
        <FormItem name="username" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
          <Input placeholder="登录名" disabled={!!editing} />
        </FormItem>
        <FormItem
          name="password"
          label="密码"
          rules={editing ? [] : [{ required: true, message: '请设置登录密码' }]}
          extra={editing ? '留空则不修改密码' : undefined}
        >
          <Input.Password placeholder={editing ? '留空不修改' : '登录密码'} autoComplete="new-password" />
        </FormItem>
        <FormItem name="nickname" label="昵称">
          <Input placeholder="显示名称" />
        </FormItem>
        <FormItem name="orgUnitId" label="所属组织">
          <Select allowClear options={orgOptions} placeholder="可选" showSearch optionFilterProp="label" />
        </FormItem>
        <FormItem name="roleIds" label="角色">
          <Select
            mode="multiple"
            allowClear
            options={roleOptions}
            placeholder="可多选；不选则使用默认角色"
            optionFilterProp="label"
          />
        </FormItem>
        <FormItem name="status" label="状态" rules={[{ required: true }]}>
          <Select
            options={[
              { value: 'ENABLED', label: '正常' },
              { value: 'DISABLED', label: '停用' },
            ]}
          />
        </FormItem>
      </FormModal>
    </div>
  );
}
