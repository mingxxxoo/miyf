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
  Switch,
  Tag,
  message,
} from 'antd';
import PageTable from '@/components/PageTable';
import { buildMenuTree, iamMenuApi, type IamMenu } from '@/modules/iam/api';

const MENU_TYPES = [
  { value: 'DIR', label: '目录' },
  { value: 'MENU', label: '菜单' },
  { value: 'BUTTON', label: '按钮' },
];

export default function MenusPage() {
  const [loading, setLoading] = useState(false);
  const [flat, setFlat] = useState<IamMenu[]>([]);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<IamMenu | null>(null);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      setFlat(await iamMenuApi.list());
    } catch {
      setFlat([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const tree = useMemo(() => buildMenuTree(flat), [flat]);

  const parentOptions = useMemo(
    () =>
      flat
        .filter((m) => m.menuType !== 'BUTTON' && (!editing || m.id !== editing.id))
        .map((m) => ({ value: m.id, label: `${m.name} (${m.menuType})` })),
    [flat, editing],
  );

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({
      menuType: 'MENU',
      status: 'ENABLED',
      visible: true,
      sortOrder: 0,
    });
    setOpen(true);
  };

  const openEdit = (row: IamMenu) => {
    setEditing(row);
    form.setFieldsValue({
      name: row.name,
      menuType: row.menuType,
      parentId: row.parentId,
      path: row.path,
      component: row.component,
      icon: row.icon,
      permissionCode: row.permissionCode,
      sortOrder: row.sortOrder ?? 0,
      visible: row.visible ?? true,
      status: row.status,
    });
    setOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    try {
      const payload = {
        name: values.name as string,
        menuType: values.menuType as string,
        parentId: values.parentId as string | undefined,
        path: values.path as string | undefined,
        component: values.component as string | undefined,
        icon: values.icon as string | undefined,
        permissionCode: values.permissionCode as string | undefined,
        sortOrder: values.sortOrder as number | undefined,
        visible: values.visible as boolean,
        status: values.status as string,
      };
      if (editing) {
        await iamMenuApi.update(editing.id, payload);
        message.success('菜单已更新');
      } else {
        await iamMenuApi.create(payload);
        message.success('菜单已创建');
      }
      setOpen(false);
      void fetchData();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await iamMenuApi.remove(id);
      message.success('已删除');
      void fetchData();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '删除失败');
    }
  };

  return (
    <div className="ck-page">
      <h2 className="ck-page-title">菜单管理</h2>
      <PageTable<IamMenu>
        title="菜单树"
        loading={loading}
        rowKey="id"
        pagination={false}
        defaultExpandAllRows
        extra={
          <Button type="primary" onClick={openCreate}>
            新增菜单
          </Button>
        }
        columns={[
          { title: '名称', dataIndex: 'name' },
          {
            title: '类型',
            dataIndex: 'menuType',
            width: 90,
            render: (v: string) => MENU_TYPES.find((t) => t.value === v)?.label || v,
          },
          { title: '路径', dataIndex: 'path', render: (v?: string) => v || '—' },
          { title: '权限码', dataIndex: 'permissionCode', render: (v?: string) => v || '—' },
          { title: '排序', dataIndex: 'sortOrder', width: 70 },
          {
            title: '可见',
            dataIndex: 'visible',
            width: 70,
            render: (v?: boolean) => (v === false ? <Tag>隐藏</Tag> : <Tag color="success">显示</Tag>),
          },
          {
            title: '状态',
            dataIndex: 'status',
            width: 80,
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
                <Popconfirm title="确认删除该菜单？" onConfirm={() => void handleDelete(row.id)}>
                  <Button type="link" size="small" danger>
                    删除
                  </Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
        dataSource={tree}
        locale={{ emptyText: '暂无菜单' }}
      />

      <Modal
        title={editing ? '编辑菜单' : '新增菜单'}
        open={open}
        width={560}
        onOk={() => void handleSubmit()}
        onCancel={() => setOpen(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="menuType" label="类型" rules={[{ required: true }]}>
            <Select options={MENU_TYPES} />
          </Form.Item>
          <Form.Item name="parentId" label="上级菜单">
            <Select allowClear options={parentOptions} placeholder="根节点留空" />
          </Form.Item>
          <Form.Item name="path" label="路由路径">
            <Input placeholder="如：/iam/users" />
          </Form.Item>
          <Form.Item name="component" label="组件">
            <Input placeholder="如：iam/Users" />
          </Form.Item>
          <Form.Item name="icon" label="图标">
            <Input placeholder="如：UserOutlined" />
          </Form.Item>
          <Form.Item name="permissionCode" label="权限码">
            <Input placeholder="如：iam:user:list" />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="visible" label="可见" valuePropName="checked">
            <Switch />
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
