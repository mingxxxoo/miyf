import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button,
  Form,
  Input,
  List,
  Popconfirm,
  Select,
  Space,
  Spin,
  Table,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  iamPermGroupApi,
  iamRoleApi,
  iamUserApi,
  type IamPermGroup,
  type IamRole,
  type IamRoleUser,
  type IamUser,
} from '@/modules/iam/api';
import { sysAppApi } from '@/modules/system/api';
import { notifyError } from '@/api/errors';
import { useSubmitting } from '@/hooks/useSubmitting';
import { USER_STATUS } from '@/constants/status';
import {
  ChangeSummaryModal,
  EmptyState,
  FormItem,
  FormModal,
  PageHeader,
  PageToolbar,
  SettingSection,
  SplitWorkspace,
  StatusBadge,
  type ChangeItem,
} from '@/ui';

const FALLBACK_PRODUCTS = [
  { value: 'system', label: '系统' },
  { value: 'kitchen', label: '厨房业务' },
  { value: 'health', label: '健康管理' },
];

const DATA_SCOPE_OPTIONS = [
  { value: 'ALL', label: '全部数据' },
  { value: 'ORG_CHILD', label: '本组织及下级' },
  { value: 'ORG', label: '仅本组织' },
  { value: 'SELF', label: '仅本人' },
];

const DATA_SCOPE_HINT: Record<string, string> = {
  ALL: '可查看与操作全部组织下的用户/组织数据',
  ORG_CHILD: '限本人所属组织及其下级组织',
  ORG: '仅限本人所属组织',
  SELF: '仅限本人账号',
};

/**
 * 角色管理：左列表 + 右详情（基本信息 / 权限组 / 关联用户 / 变更说明）。
 */
export default function RolesPage() {
  const [loading, setLoading] = useState(false);
  const [roles, setRoles] = useState<IamRole[]>([]);
  const [groups, setGroups] = useState<IamPermGroup[]>([]);
  const [productOptions, setProductOptions] = useState(FALLBACK_PRODUCTS);
  const [keyword, setKeyword] = useState('');
  const [productFilter, setProductFilter] = useState<string | undefined>();
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState('basic');

  const [createOpen, setCreateOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [form] = Form.useForm();
  const [editForm] = Form.useForm();
  const { submitting, run } = useSubmitting();

  const [roleUsers, setRoleUsers] = useState<IamRoleUser[]>([]);
  const [usersLoading, setUsersLoading] = useState(false);
  const [allUsers, setAllUsers] = useState<IamUser[]>([]);
  const [addUserOpen, setAddUserOpen] = useState(false);
  const [addUserForm] = Form.useForm();

  const [summaryOpen, setSummaryOpen] = useState(false);
  const [summaryItems, setSummaryItems] = useState<ChangeItem[]>([]);
  const [pendingEdit, setPendingEdit] = useState<{
    code: string;
    name: string;
    description?: string;
    product?: string;
    dataScope?: string;
    groupIds?: string[];
  } | null>(null);

  useEffect(() => {
    sysAppApi
      .list({ status: 'ENABLED' })
      .then((list) => {
        if (list.length) {
          setProductOptions(list.map((a) => ({ value: a.code, label: a.name })));
        }
      })
      .catch(() => {
        /* keep fallback */
      });
  }, []);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [roleList, groupList] = await Promise.all([
        iamRoleApi.list(productFilter),
        iamPermGroupApi.list(),
      ]);
      setRoles(roleList);
      setGroups(groupList);
      setSelectedId((prev) => {
        if (prev && roleList.some((r) => r.id === prev)) return prev;
        return roleList[0]?.id ?? null;
      });
    } catch (err) {
      setRoles([]);
      setGroups([]);
      notifyError(err, '加载角色失败');
    } finally {
      setLoading(false);
    }
  }, [productFilter]);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const filteredRoles = useMemo(() => {
    const q = keyword.trim().toLowerCase();
    if (!q) return roles;
    return roles.filter(
      (r) =>
        r.name.toLowerCase().includes(q) ||
        r.code.toLowerCase().includes(q) ||
        (r.description || '').toLowerCase().includes(q),
    );
  }, [roles, keyword]);

  const selected = useMemo(
    () => roles.find((r) => r.id === selectedId) ?? null,
    [roles, selectedId],
  );

  const loadRoleUsers = useCallback(async (roleId: string) => {
    setUsersLoading(true);
    try {
      setRoleUsers(await iamRoleApi.listUsers(roleId));
    } catch (err) {
      setRoleUsers([]);
      notifyError(err, '加载关联用户失败');
    } finally {
      setUsersLoading(false);
    }
  }, []);

  useEffect(() => {
    if (selectedId && activeTab === 'users') {
      void loadRoleUsers(selectedId);
    }
  }, [selectedId, activeTab, loadRoleUsers]);

  const openCreate = () => {
    form.resetFields();
    form.setFieldsValue({ product: productFilter || 'system', dataScope: 'ALL', groupIds: [] });
    setCreateOpen(true);
  };

  const openEdit = () => {
    if (!selected) return;
    editForm.setFieldsValue({
      code: selected.code,
      name: selected.name,
      description: selected.description,
      product: selected.product ?? 'system',
      dataScope: selected.dataScope ?? 'ALL',
      groupIds: selected.groupIds ?? [],
    });
    setEditOpen(true);
  };

  const handleCreate = () =>
    void run(async () => {
      const values = await form.validateFields();
      try {
        const created = await iamRoleApi.create({
          code: values.code as string,
          name: values.name as string,
          description: values.description as string | undefined,
          product: values.product as string,
          dataScope: values.dataScope as string,
          groupIds: (values.groupIds as string[]) ?? [],
        });
        message.success('角色已创建');
        setCreateOpen(false);
        setSelectedId(created.id);
        void fetchData();
      } catch (err) {
        notifyError(err, '创建失败');
      }
    });

  const prepareEditSave = async () => {
    if (!selected) return;
    const values = await editForm.validateFields();
    const next = {
      code: values.code as string,
      name: values.name as string,
      description: values.description as string | undefined,
      product: values.product as string,
      dataScope: values.dataScope as string,
      groupIds: (values.groupIds as string[]) ?? [],
    };
    const items: ChangeItem[] = [];
    if (next.code !== selected.code) {
      items.push({ label: '角色码', from: selected.code, to: next.code });
    }
    if (next.name !== selected.name) {
      items.push({ label: '名称', from: selected.name, to: next.name });
    }
    if ((next.description || '') !== (selected.description || '')) {
      items.push({
        label: '描述',
        from: selected.description || '—',
        to: next.description || '—',
      });
    }
    if ((next.product || '') !== (selected.product || '')) {
      items.push({
        label: '产品域',
        from: selected.product || '—',
        to: next.product || '—',
      });
    }
    if ((next.dataScope || 'ALL') !== (selected.dataScope || 'ALL')) {
      items.push({
        label: '数据范围',
        from: dataScopeLabel(selected.dataScope),
        to: dataScopeLabel(next.dataScope),
      });
    }
    const prevGroups = new Set(selected.groupIds ?? []);
    const nextGroups = new Set(next.groupIds ?? []);
    const added = (next.groupIds ?? []).filter((id) => !prevGroups.has(id));
    const removed = (selected.groupIds ?? []).filter((id) => !nextGroups.has(id));
    const gName = (id: string) => groups.find((g) => g.id === id)?.name ?? id;
    for (const id of added) {
      items.push({ label: `权限组「${gName(id)}」`, from: '未绑定', to: '已绑定' });
    }
    for (const id of removed) {
      items.push({ label: `权限组「${gName(id)}」`, from: '已绑定', to: '未绑定' });
    }
    if (!items.length) {
      message.info('无变更');
      return;
    }
    setPendingEdit(next);
    setSummaryItems(items);
    setSummaryOpen(true);
  };

  const confirmEdit = () =>
    void run(async () => {
      if (!selected || !pendingEdit) return;
      try {
        await iamRoleApi.update(selected.id, pendingEdit);
        message.success('角色已更新');
        setSummaryOpen(false);
        setEditOpen(false);
        setPendingEdit(null);
        void fetchData();
      } catch (err) {
        notifyError(err, '保存失败');
      }
    });

  const toggleDefault = () =>
    void run(async () => {
      if (!selected) return;
      try {
        await iamRoleApi.setDefault(selected.id, !selected.isDefault);
        message.success(selected.isDefault ? '已取消默认角色' : '已设为默认角色');
        void fetchData();
      } catch (err) {
        notifyError(err, '操作失败');
      }
    });

  const handleDelete = () =>
    void run(async () => {
      if (!selected) return;
      try {
        await iamRoleApi.remove(selected.id);
        message.success('已删除');
        setSelectedId(null);
        void fetchData();
      } catch (err) {
        notifyError(err, '删除失败');
      }
    });

  const openAddUsers = async () => {
    if (!selected) return;
    try {
      const list = await iamUserApi.list();
      setAllUsers(list);
      addUserForm.resetFields();
      addUserForm.setFieldsValue({ userIds: [] });
      setAddUserOpen(true);
    } catch (err) {
      notifyError(err, '加载用户失败');
    }
  };

  const handleAddUsers = () =>
    void run(async () => {
      if (!selected) return;
      const values = await addUserForm.validateFields();
      const userIds = (values.userIds as string[]) ?? [];
      if (!userIds.length) {
        message.warning('请选择用户');
        return;
      }
      try {
        await iamRoleApi.addUsers(selected.id, userIds);
        message.success('已添加关联用户');
        setAddUserOpen(false);
        void loadRoleUsers(selected.id);
        void fetchData();
      } catch (err) {
        notifyError(err, '添加失败');
      }
    });

  const handleRemoveUser = (userId: string) =>
    void run(async () => {
      if (!selected) return;
      try {
        await iamRoleApi.removeUser(selected.id, userId);
        message.success('已移除');
        void loadRoleUsers(selected.id);
        void fetchData();
      } catch (err) {
        notifyError(err, '移除失败');
      }
    });

  const boundUserIds = useMemo(() => new Set(roleUsers.map((u) => u.id)), [roleUsers]);

  const selectedGroups = useMemo(() => {
    const ids = new Set(selected?.groupIds ?? []);
    return groups.filter((g) => ids.has(g.id));
  }, [selected, groups]);

  const userColumns: ColumnsType<IamRoleUser> = [
    { title: '登录名', dataIndex: 'username' },
    { title: '昵称', dataIndex: 'nickname', render: (v?: string) => v || '—' },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (s?: string) => <StatusBadge code={s} map={USER_STATUS} />,
    },
    {
      title: '操作',
      width: 90,
      render: (_, row) => (
        <Popconfirm title="确认移除此用户？" onConfirm={() => handleRemoveUser(row.id)}>
          <Typography.Link type="danger">移除</Typography.Link>
        </Popconfirm>
      ),
    },
  ];

  const roleFormFields = (
    <>
      <FormItem name="code" label="角色码" rules={[{ required: true, message: '请输入角色码' }]}>
        <Input placeholder="如：kitchen_admin" />
      </FormItem>
      <FormItem name="name" label="角色名称" rules={[{ required: true, message: '请输入名称' }]}>
        <Input />
      </FormItem>
      <FormItem name="product" label="产品域" rules={[{ required: true }]}>
        <Select options={productOptions} />
      </FormItem>
      <FormItem
        name="dataScope"
        label="数据范围"
        rules={[{ required: true, message: '请选择数据范围' }]}
        extra="多角色时取最宽范围；重新登录后生效"
        full
      >
        <Select options={DATA_SCOPE_OPTIONS} />
      </FormItem>
      <FormItem name="description" label="描述" full>
        <Input.TextArea rows={2} />
      </FormItem>
      <FormItem name="groupIds" label="权限组" full>
        <Select
          mode="multiple"
          allowClear
          optionFilterProp="label"
          options={groups.map((g) => ({
            value: g.id,
            label: `${g.name} (${g.code})${g.product ? ` · ${g.product}` : ''}`,
          }))}
        />
      </FormItem>
    </>
  );

  const productLabel = (code?: string) =>
    productOptions.find((p) => p.value === code)?.label ?? code ?? '—';

  const dataScopeLabel = (code?: string) =>
    DATA_SCOPE_OPTIONS.find((p) => p.value === code)?.label ?? code ?? '全部数据';

  return (
    <div className="ck-page">
      <PageHeader
        title="角色管理"
        description="定义角色、绑定权限组，并管理角色关联用户"
      />
      <PageToolbar
        left={
          <Space wrap>
            <Input.Search
              allowClear
              placeholder="搜索角色"
              style={{ width: 200 }}
              onSearch={setKeyword}
              onChange={(e) => {
                if (!e.target.value) setKeyword('');
              }}
            />
            <Select
              allowClear
              placeholder="产品域"
              style={{ width: 140 }}
              options={productOptions}
              value={productFilter}
              onChange={(v) => setProductFilter(v)}
            />
          </Space>
        }
        right={
          <Button type="primary" onClick={openCreate}>
            新增角色
          </Button>
        }
      />

      <Spin spinning={loading}>
        <SplitWorkspace
          leftWidth={300}
          leftTitle="角色列表"
          left={
            filteredRoles.length ? (
              <List
                size="small"
                dataSource={filteredRoles}
                renderItem={(r) => (
                  <List.Item
                    key={r.id}
                    onClick={() => {
                      setSelectedId(r.id);
                      setActiveTab('basic');
                    }}
                    style={{
                      cursor: 'pointer',
                      padding: '10px 16px',
                      background: r.id === selectedId ? 'rgba(255, 179, 107, 0.12)' : undefined,
                    }}
                  >
                    <List.Item.Meta
                      title={
                        <Space size={6} wrap>
                          <span>{r.name}</span>
                          {r.isDefault ? <Tag color="red">默认</Tag> : null}
                        </Space>
                      }
                      description={
                        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                          {r.code} · {productLabel(r.product)} · {r.userCount ?? 0} 人
                        </Typography.Text>
                      }
                    />
                  </List.Item>
                )}
              />
            ) : (
              <EmptyState description={keyword ? '无匹配角色' : '暂无角色'} />
            )
          }
          rightTitle={selected ? selected.name : '角色详情'}
          rightExtra={
            selected ? (
              <Space wrap>
                <Button onClick={openEdit}>编辑</Button>
                <Button onClick={toggleDefault} loading={submitting}>
                  {selected.isDefault ? '取消默认' : '设为默认'}
                </Button>
                <Popconfirm title="确认删除该角色？" onConfirm={handleDelete}>
                  <Button danger>删除</Button>
                </Popconfirm>
              </Space>
            ) : null
          }
          right={
            !selected ? (
              <EmptyState description="请选择左侧角色" />
            ) : (
              <Tabs
                activeKey={activeTab}
                onChange={setActiveTab}
                items={[
                  {
                    key: 'basic',
                    label: '基本信息',
                    children: (
                      <Space direction="vertical" size={16} style={{ width: '100%' }}>
                        <SettingSection title="角色属性">
                          <Space direction="vertical" size={8} style={{ width: '100%' }}>
                            <InfoRow label="角色码" value={selected.code} />
                            <InfoRow label="名称" value={selected.name} />
                            <InfoRow label="产品域" value={productLabel(selected.product)} />
                            <InfoRow
                              label="默认角色"
                              value={selected.isDefault ? '是' : '否'}
                            />
                            <InfoRow label="关联用户数" value={String(selected.userCount ?? 0)} />
                            <InfoRow label="描述" value={selected.description || '—'} />
                          </Space>
                        </SettingSection>
                        <SettingSection
                          title="数据范围"
                          description={DATA_SCOPE_HINT[selected.dataScope || 'ALL']}
                        >
                          <InfoRow label="当前范围" value={dataScopeLabel(selected.dataScope)} />
                        </SettingSection>
                      </Space>
                    ),
                  },
                  {
                    key: 'groups',
                    label: '权限组',
                    children: (
                      <SettingSection
                        title="权限组（菜单 + 操作）"
                        description="角色通过权限组获得菜单与操作权限"
                      >
                        {selectedGroups.length ? (
                          <Table
                            size="small"
                            rowKey="id"
                            pagination={false}
                            dataSource={selectedGroups}
                            columns={[
                              { title: '名称', dataIndex: 'name' },
                              { title: '编码', dataIndex: 'code' },
                              {
                                title: '产品',
                                dataIndex: 'product',
                                render: (v?: string) => productLabel(v),
                              },
                              {
                                title: '说明',
                                dataIndex: 'description',
                                render: (v?: string) => v || '—',
                              },
                            ]}
                          />
                        ) : (
                          <EmptyState
                            description="尚未绑定权限组"
                            actionText="去编辑绑定"
                            onAction={openEdit}
                          />
                        )}
                      </SettingSection>
                    ),
                  },
                  {
                    key: 'users',
                    label: '关联用户',
                    children: (
                      <SettingSection
                        title="关联用户"
                        description={`当前 ${roleUsers.length} 人`}
                        extra={
                          <Space>
                            <Button
                              size="small"
                              onClick={() => {
                                if (!selected) return;
                                void run(async () => {
                                  try {
                                    await iamRoleApi.exportUsers(selected.id);
                                    message.success('已开始下载');
                                  } catch (err) {
                                    notifyError(err, '导出失败');
                                  }
                                });
                              }}
                            >
                              导出 CSV
                            </Button>
                            <Button type="primary" size="small" onClick={() => void openAddUsers()}>
                              添加用户
                            </Button>
                          </Space>
                        }
                      >
                        <Table
                          size="small"
                          rowKey="id"
                          loading={usersLoading}
                          columns={userColumns}
                          dataSource={roleUsers}
                          pagination={false}
                          locale={{ emptyText: <EmptyState description="暂无关联用户" /> }}
                        />
                      </SettingSection>
                    ),
                  },
                  {
                    key: 'audit',
                    label: '变更说明',
                    children: (
                      <SettingSection title="角色授权变更记录">
                        <EmptyState description="需表结构扩展，本期未开放专用变更记录；可在操作日志中按角色筛选查看" />
                      </SettingSection>
                    ),
                  },
                ]}
              />
            )
          }
        />
      </Spin>

      <FormModal
        title="新增角色"
        open={createOpen}
        form={form}
        confirmLoading={submitting}
        onOk={handleCreate}
        onCancel={() => setCreateOpen(false)}
      >
        {roleFormFields}
      </FormModal>

      <FormModal
        title={`编辑角色${selected ? `：${selected.name}` : ''}`}
        open={editOpen}
        form={editForm}
        confirmLoading={submitting}
        onOk={() => void prepareEditSave()}
        onCancel={() => setEditOpen(false)}
      >
        {roleFormFields}
      </FormModal>

      <FormModal
        title="添加关联用户"
        open={addUserOpen}
        form={addUserForm}
        confirmLoading={submitting}
        onOk={handleAddUsers}
        onCancel={() => setAddUserOpen(false)}
        width={520}
        columns={1}
      >
        <FormItem name="userIds" label="用户" rules={[{ required: true, message: '请选择用户' }]}>
          <Select
            mode="multiple"
            showSearch
            optionFilterProp="label"
            placeholder="选择尚未关联的用户"
            options={allUsers
              .filter((u) => !boundUserIds.has(u.id))
              .map((u) => ({
                value: u.id,
                label: `${u.nickname || u.username} (${u.username})`,
              }))}
          />
        </FormItem>
      </FormModal>

      <ChangeSummaryModal
        open={summaryOpen}
        title="确认角色变更"
        items={summaryItems}
        confirmLoading={submitting}
        onOk={() => void confirmEdit()}
        onCancel={() => {
          setSummaryOpen(false);
          setPendingEdit(null);
        }}
      />
    </div>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ display: 'flex', gap: 12 }}>
      <Typography.Text type="secondary" style={{ width: 96, flexShrink: 0 }}>
        {label}
      </Typography.Text>
      <Typography.Text>{value}</Typography.Text>
    </div>
  );
}
