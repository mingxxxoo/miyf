import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button,
  Form,
  Input,
  InputNumber,
  List,
  Modal,
  Popconfirm,
  Select,
  Space,
  Spin,
  Tabs,
  Tag,
  Typography,
  message,
} from 'antd';
import {
  iamPermGroupApi,
  iamPermissionApi,
  type IamPermGroup,
  type IamPermission,
} from '@/modules/iam/api';
import PermissionCheckTree from '@/modules/iam/components/PermissionCheckTree';
import {
  EmptyState,
  FormItem,
  FormModal,
  PageHeader,
  PageToolbar,
  SettingSection,
  SplitWorkspace,
} from '@/ui';
import { notifyError } from '@/api/errors';
import { useSubmitting } from '@/hooks/useSubmitting';

const PRODUCT_OPTIONS = [
  { value: 'basic', label: '基础' },
  { value: 'kitchen', label: '厨房业务' },
  { value: 'health', label: '健康管理' },
];

const PRODUCT_TITLE: Record<string, string> = {
  basic: '基础',
  system: '基础',
  kitchen: '厨房业务',
  health: '健康管理',
  iam: '基础',
};

/**
 * 权限组：左列表（按产品域分组）+ 右详情（基本信息 / 包含权限）。
 * 权限勾选在右侧工作区完成，避免塞进弹窗。
 */
export default function PermGroupsPage() {
  const [loading, setLoading] = useState(false);
  const [all, setAll] = useState<IamPermGroup[]>([]);
  const [permissions, setPermissions] = useState<IamPermission[]>([]);
  const [keyword, setKeyword] = useState('');
  const [productFilter, setProductFilter] = useState<string | undefined>();
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState('basic');

  const [createOpen, setCreateOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();
  const { submitting, run } = useSubmitting();

  /** 右侧权限勾选草稿（相对选中组） */
  const [draftPermIds, setDraftPermIds] = useState<string[]>([]);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [groups, perms] = await Promise.all([
        iamPermGroupApi.list(),
        iamPermissionApi.list(),
      ]);
      setAll(groups);
      setPermissions(perms);
      setSelectedId((prev) => {
        if (prev && groups.some((g) => g.id === prev)) return prev;
        return groups[0]?.id ?? null;
      });
    } catch (err) {
      setAll([]);
      setPermissions([]);
      notifyError(err, '加载权限组失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const filtered = useMemo(() => {
    const q = keyword.trim().toLowerCase();
    return all.filter((g) => {
      const product = normalizeProduct(g.product);
      if (productFilter && product !== productFilter) return false;
      if (!q) return true;
      return (
        g.name.toLowerCase().includes(q) ||
        g.code.toLowerCase().includes(q) ||
        (g.description || '').toLowerCase().includes(q)
      );
    });
  }, [all, keyword, productFilter]);

  const groupsByProduct = useMemo(() => {
    const map = new Map<string, IamPermGroup[]>();
    for (const g of filtered) {
      const product = normalizeProduct(g.product);
      if (!map.has(product)) map.set(product, []);
      map.get(product)!.push(g);
    }
    for (const list of map.values()) {
      list.sort(
        (a, b) =>
          (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.code.localeCompare(b.code),
      );
    }
    return [...map.entries()].sort((a, b) =>
      productSortKey(a[0]).localeCompare(productSortKey(b[0])),
    );
  }, [filtered]);

  const selected = useMemo(
    () => all.find((g) => g.id === selectedId) ?? null,
    [all, selectedId],
  );

  useEffect(() => {
    setDraftPermIds(selected?.permissionIds ?? []);
  }, [selected?.id, selected?.permissionIds]);

  const permDirty = useMemo(() => {
    const prev = new Set(selected?.permissionIds ?? []);
    const next = new Set(draftPermIds);
    if (prev.size !== next.size) return true;
    for (const id of next) {
      if (!prev.has(id)) return true;
    }
    return false;
  }, [selected?.permissionIds, draftPermIds]);

  const applySelect = (id: string) => {
    setSelectedId(id);
    setActiveTab('basic');
  };

  const selectGroup = (id: string) => {
    if (id === selectedId) return;
    if (permDirty) {
      Modal.confirm({
        title: '有未保存的权限变更',
        content: '切换权限组将丢弃当前勾选，是否继续？',
        okText: '丢弃并切换',
        cancelText: '取消',
        onOk: () => applySelect(id),
      });
      return;
    }
    applySelect(id);
  };

  const openCreate = () => {
    createForm.resetFields();
    createForm.setFieldsValue({
      sortOrder: 0,
      product: productFilter || 'basic',
    });
    setCreateOpen(true);
  };

  const openEdit = () => {
    if (!selected) return;
    editForm.setFieldsValue({
      code: selected.code,
      name: selected.name,
      description: selected.description,
      sortOrder: selected.sortOrder ?? 0,
      product: normalizeProduct(selected.product),
    });
    setEditOpen(true);
  };

  const handleCreate = () =>
    void run(async () => {
      const values = await createForm.validateFields();
      try {
        const created = await iamPermGroupApi.create({
          code: values.code as string,
          name: values.name as string,
          description: values.description as string | undefined,
          sortOrder: values.sortOrder as number | undefined,
          product: values.product as string | undefined,
          permissionIds: [],
        });
        message.success('权限组已创建，可继续勾选权限');
        setCreateOpen(false);
        setSelectedId(created.id);
        setActiveTab('permissions');
        void fetchData();
      } catch (err) {
        notifyError(err, '创建失败');
      }
    });

  const handleEdit = () =>
    void run(async () => {
      if (!selected) return;
      const values = await editForm.validateFields();
      try {
        await iamPermGroupApi.update(selected.id, {
          code: values.code as string,
          name: values.name as string,
          description: values.description as string | undefined,
          sortOrder: values.sortOrder as number | undefined,
          product: values.product as string | undefined,
          permissionIds: selected.permissionIds ?? [],
        });
        message.success('基本信息已更新');
        setEditOpen(false);
        void fetchData();
      } catch (err) {
        notifyError(err, '保存失败');
      }
    });

  const handleSavePermissions = () =>
    void run(async () => {
      if (!selected) return;
      try {
        await iamPermGroupApi.update(selected.id, {
          code: selected.code,
          name: selected.name,
          description: selected.description,
          sortOrder: selected.sortOrder,
          product: selected.product,
          permissionIds: draftPermIds,
        });
        message.success('权限已保存');
        void fetchData();
      } catch (err) {
        notifyError(err, '保存权限失败');
      }
    });

  const handleResetPermissions = () => {
    setDraftPermIds(selected?.permissionIds ?? []);
  };

  const handleDelete = () =>
    void run(async () => {
      if (!selected) return;
      try {
        await iamPermGroupApi.remove(selected.id);
        message.success('已删除');
        setSelectedId(null);
        void fetchData();
      } catch (err) {
        notifyError(err, '删除失败');
      }
    });

  const productLabel = (code?: string) =>
    PRODUCT_TITLE[normalizeProduct(code)] || code || '—';

  const basicFields = (codeDisabled: boolean) => (
    <>
      <FormItem name="code" label="编码" rules={[{ required: true, message: '请输入编码' }]}>
        <Input placeholder="如：iam_user" disabled={codeDisabled} />
      </FormItem>
      <FormItem name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
        <Input placeholder="权限组名称" />
      </FormItem>
      <FormItem name="product" label="产品域" rules={[{ required: true }]}>
        <Select options={PRODUCT_OPTIONS} />
      </FormItem>
      <FormItem name="sortOrder" label="排序">
        <InputNumber style={{ width: '100%' }} min={0} />
      </FormItem>
      <FormItem name="description" label="说明" full>
        <Input.TextArea rows={2} placeholder="可选说明" />
      </FormItem>
    </>
  );

  return (
    <div className="ck-page">
      <PageHeader title="权限组" description="按产品域聚合可分配的权限集合，供角色绑定" />
      <PageToolbar
        left={
          <Space wrap>
            <Input.Search
              allowClear
              placeholder="搜索名称 / 编码"
              style={{ width: 220 }}
              onSearch={setKeyword}
              onChange={(e) => {
                if (!e.target.value) setKeyword('');
              }}
            />
            <Select
              allowClear
              placeholder="产品域"
              style={{ width: 140 }}
              options={PRODUCT_OPTIONS}
              value={productFilter}
              onChange={(v) => setProductFilter(v)}
            />
          </Space>
        }
        right={
          <Button type="primary" onClick={openCreate}>
            添加权限组
          </Button>
        }
      />

      <Spin spinning={loading}>
        <SplitWorkspace
          leftWidth={300}
          leftTitle="权限组"
          leftExtra={
            <Typography.Text type="secondary" style={{ fontSize: 12 }}>
              {filtered.length} 个
            </Typography.Text>
          }
          left={
            filtered.length ? (
              <div>
                {groupsByProduct.map(([product, list]) => (
                  <div key={product} style={{ marginBottom: 8 }}>
                    <Typography.Text
                      type="secondary"
                      style={{
                        display: 'block',
                        padding: '8px 16px 4px',
                        fontSize: 12,
                        fontWeight: 600,
                      }}
                    >
                      {productLabel(product)}
                    </Typography.Text>
                    <List
                      size="small"
                      dataSource={list}
                      renderItem={(g) => (
                        <List.Item
                          key={g.id}
                          onClick={() => selectGroup(g.id)}
                          style={{
                            cursor: 'pointer',
                            padding: '10px 16px',
                            background:
                              g.id === selectedId ? 'rgba(255, 179, 107, 0.12)' : undefined,
                          }}
                        >
                          <List.Item.Meta
                            title={<span>{g.name}</span>}
                            description={
                              <Space size={6} wrap>
                                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                  {g.code}
                                </Typography.Text>
                                <Tag style={{ margin: 0, fontSize: 11, lineHeight: '18px' }}>
                                  {(g.permissionIds?.length ?? 0)} 项权限
                                </Tag>
                              </Space>
                            }
                          />
                        </List.Item>
                      )}
                    />
                  </div>
                ))}
              </div>
            ) : (
              <EmptyState description={keyword || productFilter ? '无匹配权限组' : '暂无权限组'} />
            )
          }
          rightTitle={selected ? selected.name : '权限组详情'}
          rightExtra={
            selected ? (
              <Space wrap>
                <Button onClick={openEdit}>编辑信息</Button>
                <Popconfirm title="确认删除该权限组？" onConfirm={handleDelete}>
                  <Button danger>删除</Button>
                </Popconfirm>
              </Space>
            ) : null
          }
          right={
            !selected ? (
              <EmptyState description="请选择左侧权限组，或新建一个" />
            ) : (
              <Tabs
                activeKey={activeTab}
                onChange={setActiveTab}
                items={[
                  {
                    key: 'basic',
                    label: '基本信息',
                    children: (
                      <SettingSection
                        title="组属性"
                        description="编码创建后不可修改；产品域决定可选权限范围"
                      >
                        <Space direction="vertical" size={10} style={{ width: '100%' }}>
                          <InfoRow label="编码" value={selected.code} />
                          <InfoRow label="名称" value={selected.name} />
                          <InfoRow label="产品域" value={productLabel(selected.product)} />
                          <InfoRow label="排序" value={String(selected.sortOrder ?? 0)} />
                          <InfoRow
                            label="权限数"
                            value={`${selected.permissionIds?.length ?? 0} 项`}
                          />
                          <InfoRow label="说明" value={selected.description || '—'} />
                        </Space>
                      </SettingSection>
                    ),
                  },
                  {
                    key: 'permissions',
                    label: (
                      <Space size={6}>
                        <span>包含权限</span>
                        {permDirty ? <Tag color="orange">未保存</Tag> : null}
                      </Space>
                    ),
                    children: (
                      <SettingSection
                        title="接口权限"
                        description="按 产品 → 业务模块 → 接口 勾选；仅接口权限会写入权限组"
                        extra={
                          <Space>
                            <Button disabled={!permDirty} onClick={handleResetPermissions}>
                              重置
                            </Button>
                            <Button
                              type="primary"
                              disabled={!permDirty}
                              loading={submitting}
                              onClick={handleSavePermissions}
                            >
                              保存权限
                            </Button>
                          </Space>
                        }
                      >
                        <PermissionCheckTree
                          value={draftPermIds}
                          onChange={setDraftPermIds}
                          permissions={permissions}
                          productFilter={normalizeProduct(selected.product)}
                          height={420}
                        />
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
        title="添加权限组"
        open={createOpen}
        form={createForm}
        confirmLoading={submitting}
        onOk={handleCreate}
        onCancel={() => setCreateOpen(false)}
      >
        {basicFields(false)}
      </FormModal>

      <FormModal
        title={selected ? `编辑权限组：${selected.name}` : '编辑权限组'}
        open={editOpen}
        form={editForm}
        confirmLoading={submitting}
        onOk={handleEdit}
        onCancel={() => setEditOpen(false)}
      >
        {basicFields(true)}
      </FormModal>
    </div>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
      <Typography.Text type="secondary" style={{ width: 72, flexShrink: 0 }}>
        {label}
      </Typography.Text>
      <Typography.Text style={{ wordBreak: 'break-all' }}>{value}</Typography.Text>
    </div>
  );
}

function normalizeProduct(product?: string) {
  if (!product) return 'basic';
  const p = product.toLowerCase();
  if (p === 'iam' || p === 'system') return 'basic';
  return p;
}

function productSortKey(product: string) {
  const order: Record<string, string> = {
    basic: '0',
    system: '1',
    kitchen: '2',
    health: '3',
  };
  return (order[product] || '9') + product;
}
