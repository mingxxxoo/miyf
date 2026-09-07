import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Spin,
  Switch,
  Tree,
  Typography,
  message,
} from 'antd';
import type { DataNode } from 'antd/es/tree';
import {
  buildMenuTree,
  iamMenuApi,
  type IamMenu,
} from '@/modules/iam/api';
import { sysAppApi, type SysApp } from '@/modules/system/api';
import { notifyError } from '@/api/errors';
import { useSubmitting } from '@/hooks/useSubmitting';
import { ENABLED_STATUS } from '@/constants/status';
import {
  EmptyState,
  FormItem,
  FormGrid,
  PageHeader,
  PageToolbar,
  SettingSection,
  SplitWorkspace,
  StatusBadge,
} from '@/ui';

const MENU_TYPES = [
  { value: 'DIR', label: '目录' },
  { value: 'MENU', label: '菜单' },
  { value: 'BUTTON', label: '按钮' },
];

type FormValues = {
  name: string;
  menuType: string;
  parentId?: string;
  path?: string;
  component?: string;
  icon?: string;
  permissionCode?: string;
  product?: string;
  sortOrder?: number;
  visible?: boolean;
  status?: string;
};

/**
 * 菜单管理：左树右表单；排序仅支持编辑 sortOrder（无拖拽接口）。
 */
export default function MenusPage() {
  const [loading, setLoading] = useState(false);
  const [flat, setFlat] = useState<IamMenu[]>([]);
  const [keyword, setKeyword] = useState('');
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [mode, setMode] = useState<'view' | 'create'>('view');
  const [createDefaults, setCreateDefaults] = useState<Partial<FormValues>>({});
  const [dirty, setDirty] = useState(false);
  const [form] = Form.useForm<FormValues>();
  const { submitting, run } = useSubmitting();
  const [apps, setApps] = useState<SysApp[]>([]);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [list, appList] = await Promise.all([
        iamMenuApi.list(),
        sysAppApi.list({ status: 'ENABLED' }).catch(() => [] as SysApp[]),
      ]);
      setFlat(list);
      setApps(appList);
      setSelectedId((prev) => {
        if (prev && list.some((m) => m.id === prev)) return prev;
        return list[0]?.id ?? null;
      });
    } catch (err) {
      setFlat([]);
      notifyError(err, '加载菜单失败');
    } finally {
      setLoading(false);
    }
  }, []);

  const productOptions = useMemo(() => {
    const fromApps = apps.map((a) => ({ value: a.code, label: `${a.name} (${a.code})` }));
    if (fromApps.length) return fromApps;
    return [
      { value: 'system', label: '系统 (system)' },
      { value: 'kitchen', label: '厨房 (kitchen)' },
      { value: 'health', label: '健康 (health)' },
    ];
  }, [apps]);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const filteredFlat = useMemo(() => {
    const q = keyword.trim().toLowerCase();
    if (!q) return flat;
    const matched = new Set(
      flat
        .filter(
          (m) =>
            m.name.toLowerCase().includes(q) ||
            (m.path || '').toLowerCase().includes(q) ||
            (m.permissionCode || '').toLowerCase().includes(q),
        )
        .map((m) => m.id),
    );
    if (!matched.size) return [];
    const byId = new Map(flat.map((m) => [m.id, m]));
    const keep = new Set(matched);
    for (const id of matched) {
      let cur = byId.get(id);
      while (cur?.parentId) {
        keep.add(cur.parentId);
        cur = byId.get(cur.parentId);
      }
    }
    return flat.filter((m) => keep.has(m.id));
  }, [flat, keyword]);

  const tree = useMemo(() => buildMenuTree(filteredFlat), [filteredFlat]);

  const selected = useMemo(
    () => (mode === 'view' && selectedId ? flat.find((m) => m.id === selectedId) ?? null : null),
    [flat, selectedId, mode],
  );

  const childCount = useMemo(
    () => (selectedId ? flat.filter((m) => m.parentId === selectedId).length : 0),
    [flat, selectedId],
  );

  const parentOptions = useMemo(() => {
    const excludeId = mode === 'view' ? selectedId : undefined;
    const excluded = new Set<string>();
    if (excludeId) {
      excluded.add(excludeId);
      const childrenOf = (parentId: string) => {
        for (const m of flat) {
          if (m.parentId === parentId && !excluded.has(m.id)) {
            excluded.add(m.id);
            childrenOf(m.id);
          }
        }
      };
      childrenOf(excludeId);
    }
    return flat
      .filter((m) => m.menuType !== 'BUTTON' && !excluded.has(m.id))
      .map((m) => ({ value: m.id, label: `${m.name} (${m.menuType})` }));
  }, [flat, mode, selectedId]);

  const fillFormFromMenu = useCallback(
    (row: IamMenu) => {
      form.setFieldsValue({
        name: row.name,
        menuType: row.menuType,
        parentId: row.parentId,
        path: row.path,
        component: row.component,
        icon: row.icon,
        permissionCode: row.permissionCode,
        product: row.product || 'system',
        sortOrder: row.sortOrder ?? 0,
        visible: row.visible ?? true,
        status: row.status,
      });
      setDirty(false);
    },
    [form],
  );

  useEffect(() => {
    if (mode === 'create') {
      form.resetFields();
      form.setFieldsValue({
        menuType: 'MENU',
        status: 'ENABLED',
        visible: true,
        sortOrder: 0,
        product: 'system',
        ...createDefaults,
      });
      setDirty(false);
      return;
    }
    if (selected) {
      fillFormFromMenu(selected);
    } else {
      form.resetFields();
      setDirty(false);
    }
  }, [mode, selected, createDefaults, fillFormFromMenu, form]);

  const treeData = useMemo(() => toTreeData(tree), [tree]);

  const confirmIfDirty = (next: () => void) => {
    if (!dirty) {
      next();
      return;
    }
    Modal.confirm({
      title: '有未保存的修改',
      content: '切换后将丢失当前编辑内容，是否继续？',
      okText: '放弃修改',
      cancelText: '继续编辑',
      onOk: () => {
        setDirty(false);
        next();
      },
    });
  };

  const selectNode = (id: string) => {
    confirmIfDirty(() => {
      setMode('view');
      setSelectedId(id);
    });
  };

  const startCreate = (defaults: Partial<FormValues>) => {
    confirmIfDirty(() => {
      setCreateDefaults(defaults);
      setMode('create');
    });
  };

  const addRoot = () =>
    startCreate({
      menuType: 'MENU',
      parentId: undefined,
      product: 'system',
      status: 'ENABLED',
      visible: true,
      sortOrder: 0,
    });

  const addChild = () => {
    if (!selectedId || !selected) {
      message.warning('请先选择父级菜单');
      return;
    }
    if (selected.menuType === 'BUTTON') {
      message.warning('按钮节点下不能再添加子项');
      return;
    }
    startCreate({
      menuType: 'MENU',
      parentId: selectedId,
      product: selected.product || 'system',
      status: 'ENABLED',
      visible: true,
      sortOrder: 0,
    });
  };

  const addButton = () => {
    if (!selectedId || !selected) {
      message.warning('请先选择父级菜单');
      return;
    }
    if (selected.menuType === 'BUTTON') {
      message.warning('请选择目录或菜单作为按钮父级');
      return;
    }
    startCreate({
      menuType: 'BUTTON',
      parentId: selectedId,
      product: selected.product || 'system',
      status: 'ENABLED',
      visible: true,
      sortOrder: 0,
    });
  };

  const handleSave = () =>
    void run(async () => {
      const values = await form.validateFields();
      const payload = {
        name: values.name,
        menuType: values.menuType,
        parentId: values.parentId || undefined,
        path: values.path,
        component: values.component,
        icon: values.icon,
        permissionCode: values.permissionCode,
        product: values.product || 'system',
        sortOrder: values.sortOrder ?? 0,
        visible: values.visible ?? true,
        status: values.status ?? 'ENABLED',
      };
      try {
        if (mode === 'create') {
          const created = await iamMenuApi.create(payload);
          message.success('菜单已创建');
          setMode('view');
          setSelectedId(created.id);
          setDirty(false);
        } else if (selectedId) {
          await iamMenuApi.update(selectedId, payload);
          message.success('菜单已更新');
          setDirty(false);
        }
        void fetchData();
      } catch (err) {
        notifyError(err, '保存失败');
      }
    });

  const handleDelete = () => {
    if (!selectedId || !selected) return;
    if (childCount > 0) {
      message.error(`该节点下还有 ${childCount} 个子项，请先删除子项`);
      return;
    }
    Modal.confirm({
      title: '确认删除该菜单？',
      content: selected.name,
      okType: 'danger',
      onOk: () =>
        run(async () => {
          try {
            await iamMenuApi.remove(selectedId);
            message.success('已删除');
            setSelectedId(null);
            setDirty(false);
            void fetchData();
          } catch (err) {
            notifyError(err, '删除失败');
          }
        }),
    });
  };

  const handleReset = () => {
    if (mode === 'create') {
      form.resetFields();
      form.setFieldsValue({
        menuType: 'MENU',
        status: 'ENABLED',
        visible: true,
        sortOrder: 0,
        product: 'system',
        ...createDefaults,
      });
      setDirty(false);
      return;
    }
    if (selected) fillFormFromMenu(selected);
  };

  return (
    <div className="ck-page">
      <PageHeader
        title="菜单管理"
        description="维护系统菜单树；排序请修改表单中的「排序」字段（暂无拖拽排序接口）"
      />
      <PageToolbar
        left={
          <Input.Search
            allowClear
            placeholder="搜索名称 / 路径 / 权限码"
            style={{ width: 260 }}
            onSearch={setKeyword}
            onChange={(e) => {
              if (!e.target.value) setKeyword('');
            }}
          />
        }
        right={
          <Space wrap>
            <Button onClick={addRoot}>新增根菜单</Button>
            <Button disabled={!selectedId || mode === 'create'} onClick={addChild}>
              新增子菜单
            </Button>
            <Button disabled={!selectedId || mode === 'create'} onClick={addButton}>
              新增按钮
            </Button>
            <Button
              danger
              disabled={!selectedId || mode === 'create' || submitting}
              onClick={handleDelete}
            >
              删除
            </Button>
          </Space>
        }
      />

      <Spin spinning={loading}>
        <SplitWorkspace
          leftWidth={320}
          leftTitle="菜单树"
          left={
            treeData.length ? (
              <Tree
                showLine={{ showLeafIcon: false }}
                treeData={treeData}
                selectedKeys={mode === 'view' && selectedId ? [selectedId] : []}
                defaultExpandAll
                onSelect={(keys) => {
                  const id = keys[0] != null ? String(keys[0]) : null;
                  if (id) selectNode(id);
                }}
                style={{ padding: '8px 12px' }}
              />
            ) : (
              <EmptyState description={keyword ? '无匹配菜单' : '暂无菜单'} />
            )
          }
          rightTitle={
            mode === 'create'
              ? `新增${createDefaults.menuType === 'BUTTON' ? '按钮' : '菜单'}`
              : selected
                ? `编辑 · ${selected.name}`
                : '菜单详情'
          }
          rightExtra={
            selected || mode === 'create' ? (
              <Space>
                <Button disabled={!dirty || submitting} onClick={handleReset}>
                  重置
                </Button>
                <Button type="primary" loading={submitting} disabled={!dirty && mode !== 'create'} onClick={handleSave}>
                  保存
                </Button>
              </Space>
            ) : null
          }
          right={
            !selected && mode !== 'create' ? (
              <EmptyState description="请选择左侧菜单节点" />
            ) : (
              <SettingSection
                title="菜单属性"
                description={
                  mode === 'create'
                    ? '填写后保存即可创建'
                    : '修改后记得保存；拖拽排序未开放，请调整「排序」数值'
                }
              >
                {selected && mode === 'view' ? (
                  <Space style={{ marginBottom: 12 }} wrap>
                    <StatusBadge code={selected.status} map={ENABLED_STATUS} />
                    <Typography.Text type="secondary">
                      类型：{MENU_TYPES.find((t) => t.value === selected.menuType)?.label || selected.menuType}
                    </Typography.Text>
                    {childCount > 0 ? (
                      <Typography.Text type="secondary">子节点：{childCount}</Typography.Text>
                    ) : null}
                  </Space>
                ) : null}
                <Form
                  form={form}
                  layout="vertical"
                  onValuesChange={() => setDirty(true)}
                >
                  <FormGrid columns={2}>
                    <FormItem name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
                      <Input />
                    </FormItem>
                    <FormItem name="menuType" label="类型" rules={[{ required: true }]}>
                      <Select options={MENU_TYPES} />
                    </FormItem>
                    <FormItem name="parentId" label="上级菜单" full>
                      <Select allowClear options={parentOptions} placeholder="根节点留空" />
                    </FormItem>
                    <FormItem name="path" label="路由路径">
                      <Input placeholder="如：/system/role-auth" />
                    </FormItem>
                    <FormItem name="component" label="组件">
                      <Input placeholder="如：system/RoleAuth" />
                    </FormItem>
                    <FormItem name="icon" label="图标">
                      <Input placeholder="如：UserOutlined" />
                    </FormItem>
                    <FormItem name="permissionCode" label="权限码">
                      <Input placeholder="如：iam:role:list" />
                    </FormItem>
                    <FormItem name="product" label="所属应用" rules={[{ required: true, message: '请选择应用' }]}>
                      <Select options={productOptions} placeholder="选择产品域" />
                    </FormItem>
                    <FormItem
                      name="sortOrder"
                      label="排序"
                      extra="暂无拖拽排序接口，请在此调整数值后保存"
                    >
                      <InputNumber style={{ width: '100%' }} min={0} />
                    </FormItem>
                    <FormItem name="visible" label="可见" valuePropName="checked">
                      <Switch />
                    </FormItem>
                    <FormItem name="status" label="状态" rules={[{ required: true }]}>
                      <Select
                        options={[
                          { value: 'ENABLED', label: '启用' },
                          { value: 'DISABLED', label: '停用' },
                        ]}
                      />
                    </FormItem>
                  </FormGrid>
                </Form>
              </SettingSection>
            )
          }
        />
      </Spin>
    </div>
  );
}

function toTreeData(nodes: IamMenu[]): DataNode[] {
  return nodes.map((n) => ({
    key: n.id,
    title: (
      <Space size={6}>
        <span>{n.name}</span>
        <Typography.Text type="secondary" style={{ fontSize: 12 }}>
          {MENU_TYPES.find((t) => t.value === n.menuType)?.label || n.menuType}
        </Typography.Text>
      </Space>
    ),
    children: n.children?.length ? toTreeData(n.children) : undefined,
  }));
}
