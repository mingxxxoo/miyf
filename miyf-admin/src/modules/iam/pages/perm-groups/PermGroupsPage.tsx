import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button,
  Form,
  Input,
  InputNumber,
  Popconfirm,
  Select,
  Space,
  Tree,
  Typography,
  message,
} from 'antd';
import type { DataNode } from 'antd/es/tree';
import {
  iamPermGroupApi,
  iamPermissionApi,
  type IamPermGroup,
  type IamPermission,
} from '@/modules/iam/api';
import PermissionCheckTree from '@/modules/iam/components/PermissionCheckTree';
import { FormItem, FormModal, PageHeader, EmptyState, SettingSection } from '@/ui';
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
 * 权限组：按 product → 组 两级树 + 搜索 + 添加；编辑时用树勾选权限。
 */
export default function PermGroupsPage() {
  const [loading, setLoading] = useState(false);
  const [all, setAll] = useState<IamPermGroup[]>([]);
  const [permissions, setPermissions] = useState<IamPermission[]>([]);
  const [keyword, setKeyword] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<IamPermGroup | null>(null);
  const [form] = Form.useForm();
  const { submitting, run } = useSubmitting();
  const productWatch = Form.useWatch('product', form);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [groups, perms] = await Promise.all([iamPermGroupApi.list(), iamPermissionApi.list()]);
      setAll(groups);
      setPermissions(perms);
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
    if (!q) return all;
    return all.filter(
      (g) =>
        g.name.toLowerCase().includes(q) ||
        g.code.toLowerCase().includes(q) ||
        (g.description || '').toLowerCase().includes(q),
    );
  }, [all, keyword]);

  const openEdit = useCallback(
    (row: IamPermGroup) => {
      setEditing(row);
      form.setFieldsValue({
        code: row.code,
        name: row.name,
        description: row.description,
        sortOrder: row.sortOrder ?? 0,
        product: normalizeProduct(row.product),
        permissionIds: row.permissionIds ?? [],
      });
      setOpen(true);
    },
    [form],
  );

  const handleDelete = useCallback(
    async (id: string) => {
      await run(async () => {
        try {
          await iamPermGroupApi.remove(id);
          message.success('已删除');
          void fetchData();
        } catch (err) {
          notifyError(err, '删除失败');
        }
      });
    },
    [fetchData, run],
  );

  const treeData = useMemo(() => {
    const byProduct = new Map<string, IamPermGroup[]>();
    for (const g of filtered) {
      const product = normalizeProduct(g.product);
      if (!byProduct.has(product)) byProduct.set(product, []);
      byProduct.get(product)!.push(g);
    }
    const roots: DataNode[] = [];
    for (const [product, groups] of [...byProduct.entries()].sort((a, b) =>
      productSortKey(a[0]).localeCompare(productSortKey(b[0])),
    )) {
      groups.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.code.localeCompare(b.code));
      roots.push({
        key: `product:${product}`,
        title: PRODUCT_TITLE[product] || `${product} 工作台`,
        children: groups.map((g) => ({
          key: g.id,
          title: (
            <Space>
              <span>{g.name}</span>
              <Typography.Text type="secondary">({g.code})</Typography.Text>
              <Typography.Link
                onClick={(e) => {
                  e.stopPropagation();
                  openEdit(g);
                }}
              >
                编辑
              </Typography.Link>
              <Popconfirm title="确认删除该权限组？" onConfirm={() => void handleDelete(g.id)}>
                <Typography.Link type="danger" onClick={(e) => e.stopPropagation()}>
                  删除
                </Typography.Link>
              </Popconfirm>
            </Space>
          ),
          isLeaf: true,
        })),
      });
    }
    return roots;
  }, [filtered, openEdit, handleDelete]);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ sortOrder: 0, product: 'basic', permissionIds: [] });
    setOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    await run(async () => {
      try {
        const payload = {
          code: values.code as string,
          name: values.name as string,
          description: values.description as string | undefined,
          sortOrder: values.sortOrder as number | undefined,
          product: values.product as string | undefined,
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
        notifyError(err, '保存失败');
      }
    });
  };

  return (
    <div className="ck-page">
      <PageHeader title="权限组" description="按产品聚合可分配的权限集合" />
      <SettingSection
        title="权限组列表"
        extra={
          <Space>
            <Input.Search
              allowClear
              placeholder="请输入权限组名称"
              style={{ width: 240 }}
              onSearch={setKeyword}
              onChange={(e) => {
                if (!e.target.value) setKeyword('');
              }}
            />
            <Button type="primary" onClick={openCreate}>
              添加权限组
            </Button>
          </Space>
        }
      >
        {loading ? (
          <Typography.Text type="secondary">加载中…</Typography.Text>
        ) : treeData.length === 0 ? (
          <EmptyState description="暂无权限组" />
        ) : (
          <Tree showLine defaultExpandAll treeData={treeData} />
        )}
      </SettingSection>

      <FormModal
        title={editing ? '编辑权限组' : '添加权限组'}
        open={open}
        form={form}
        confirmLoading={submitting}
        onOk={() => void handleSubmit()}
        onCancel={() => setOpen(false)}
        width={720}
      >
        <FormItem name="code" label="编码" rules={[{ required: true }]}>
          <Input placeholder="如：iam_user" disabled={!!editing} />
        </FormItem>
        <FormItem name="name" label="名称" rules={[{ required: true }]}>
          <Input />
        </FormItem>
        <FormItem name="product" label="产品域" rules={[{ required: true }]}>
          <Select options={PRODUCT_OPTIONS} />
        </FormItem>
        <FormItem name="sortOrder" label="排序">
          <InputNumber style={{ width: '100%' }} min={0} />
        </FormItem>
        <FormItem name="description" label="说明" full>
          <Input.TextArea rows={2} />
        </FormItem>
        <FormItem
          name="permissionIds"
          label="包含权限"
          full
          extra="按 产品 → 业务模块 → 接口 勾选；仅接口权限会写入权限组"
        >
          <PermissionCheckTree
            permissions={permissions}
            productFilter={productWatch ? String(productWatch) : undefined}
            height={380}
          />
        </FormItem>
      </FormModal>
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
