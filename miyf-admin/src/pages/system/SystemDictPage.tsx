import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Col,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import PageTable from '@/components/PageTable';
import { sysDictApi, type SysDictItem, type SysDictType } from '@/pages/system/api';

/**
 * 数据字典 CRUD：左侧类型，右侧字典项。
 */
export default function SystemDictPage() {
  const [typeLoading, setTypeLoading] = useState(false);
  const [itemLoading, setItemLoading] = useState(false);
  const [types, setTypes] = useState<SysDictType[]>([]);
  const [items, setItems] = useState<SysDictItem[]>([]);
  const [selectedTypeId, setSelectedTypeId] = useState<string | null>(null);
  const [typeKeyword, setTypeKeyword] = useState('');
  const [typeOpen, setTypeOpen] = useState(false);
  const [itemOpen, setItemOpen] = useState(false);
  const [editingType, setEditingType] = useState<SysDictType | null>(null);
  const [editingItem, setEditingItem] = useState<SysDictItem | null>(null);
  const [typeForm] = Form.useForm();
  const [itemForm] = Form.useForm();

  const selectedType = types.find((t) => t.id === selectedTypeId) ?? null;

  const fetchTypes = useCallback(async () => {
    setTypeLoading(true);
    try {
      const list = await sysDictApi.listTypes(typeKeyword.trim() || undefined);
      setTypes(list);
      setSelectedTypeId((prev) => {
        if (prev && list.some((t) => t.id === prev)) return prev;
        return list[0]?.id ?? null;
      });
    } catch (err) {
      setTypes([]);
      message.error(err instanceof Error ? err.message : '加载字典类型失败');
    } finally {
      setTypeLoading(false);
    }
  }, [typeKeyword]);

  const fetchItems = useCallback(async (typeId: string) => {
    setItemLoading(true);
    try {
      const list = await sysDictApi.listItems(typeId);
      setItems(list);
    } catch (err) {
      setItems([]);
      message.error(err instanceof Error ? err.message : '加载字典项失败');
    } finally {
      setItemLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchTypes();
  }, [fetchTypes]);

  useEffect(() => {
    if (selectedTypeId) {
      void fetchItems(selectedTypeId);
    } else {
      setItems([]);
    }
  }, [selectedTypeId, fetchItems]);

  const openCreateType = () => {
    setEditingType(null);
    typeForm.resetFields();
    typeForm.setFieldsValue({ status: 'ENABLED', sortOrder: 0 });
    setTypeOpen(true);
  };

  const openEditType = (row: SysDictType) => {
    setEditingType(row);
    typeForm.setFieldsValue({
      code: row.code,
      name: row.name,
      description: row.description,
      status: row.status,
      sortOrder: row.sortOrder ?? 0,
    });
    setTypeOpen(true);
  };

  const submitType = async () => {
    const values = await typeForm.validateFields();
    const payload = {
      code: values.code as string,
      name: values.name as string,
      description: values.description as string | undefined,
      status: values.status as string,
      sortOrder: values.sortOrder as number | undefined,
    };
    try {
      if (editingType) {
        await sysDictApi.updateType(editingType.id, payload);
        message.success('字典类型已更新');
      } else {
        const created = await sysDictApi.createType(payload);
        message.success('字典类型已创建');
        setSelectedTypeId(created.id);
      }
      setTypeOpen(false);
      void fetchTypes();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    }
  };

  const deleteType = async (id: string) => {
    try {
      await sysDictApi.removeType(id);
      message.success('已删除');
      if (selectedTypeId === id) setSelectedTypeId(null);
      void fetchTypes();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '删除失败');
    }
  };

  const openCreateItem = () => {
    if (!selectedTypeId) {
      message.warning('请先选择字典类型');
      return;
    }
    setEditingItem(null);
    itemForm.resetFields();
    itemForm.setFieldsValue({ status: 'ENABLED', sortOrder: 0, typeId: selectedTypeId });
    setItemOpen(true);
  };

  const openEditItem = (row: SysDictItem) => {
    setEditingItem(row);
    itemForm.setFieldsValue({
      typeId: row.typeId,
      itemValue: row.itemValue,
      itemLabel: row.itemLabel,
      sortOrder: row.sortOrder ?? 0,
      status: row.status,
      remark: row.remark,
    });
    setItemOpen(true);
  };

  const submitItem = async () => {
    const values = await itemForm.validateFields();
    const payload = {
      typeId: (values.typeId as string) || selectedTypeId!,
      itemValue: values.itemValue as string,
      itemLabel: values.itemLabel as string,
      sortOrder: values.sortOrder as number | undefined,
      status: values.status as string,
      remark: values.remark as string | undefined,
    };
    try {
      if (editingItem) {
        await sysDictApi.updateItem(editingItem.id, payload);
        message.success('字典项已更新');
      } else {
        await sysDictApi.createItem(payload);
        message.success('字典项已创建');
      }
      setItemOpen(false);
      if (selectedTypeId) void fetchItems(selectedTypeId);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    }
  };

  const deleteItem = async (id: string) => {
    try {
      await sysDictApi.removeItem(id);
      message.success('已删除');
      if (selectedTypeId) void fetchItems(selectedTypeId);
    } catch (err) {
      message.error(err instanceof Error ? err.message : '删除失败');
    }
  };

  const typeColumns: ColumnsType<SysDictType> = [
    {
      title: '编码',
      dataIndex: 'code',
      ellipsis: true,
    },
    {
      title: '名称',
      dataIndex: 'name',
      ellipsis: true,
    },
    {
      title: '操作',
      width: 120,
      render: (_, row) => (
        <Space size={0}>
          <Button type="link" size="small" onClick={() => openEditType(row)}>
            编辑
          </Button>
          <Popconfirm title="删除类型将级联删除字典项，确认？" onConfirm={() => void deleteType(row.id)}>
            <Button type="link" size="small" danger>
              删
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div className="ck-page">
      <h2 className="ck-page-title">数据字典</h2>
      <Typography.Paragraph type="secondary" style={{ marginTop: 0 }}>
        维护字典类型与字典项；业务下拉可按类型编码拉取启用项。
      </Typography.Paragraph>
      <Row gutter={16}>
        <Col xs={24} lg={9}>
          <PageTable<SysDictType>
            title="字典类型"
            loading={typeLoading}
            rowKey="id"
            size="small"
            extra={
              <Space wrap>
                <Input.Search
                  allowClear
                  placeholder="编码 / 名称"
                  style={{ width: 160 }}
                  onSearch={(v) => setTypeKeyword(v)}
                />
                <Button type="primary" onClick={openCreateType}>
                  新增
                </Button>
              </Space>
            }
            columns={typeColumns}
            dataSource={types}
            pagination={false}
            locale={{ emptyText: '暂无字典类型' }}
            rowClassName={(row) => (row.id === selectedTypeId ? 'ant-table-row-selected' : '')}
            onRow={(row) => ({
              onClick: () => setSelectedTypeId(row.id),
              style: { cursor: 'pointer' },
            })}
          />
        </Col>
        <Col xs={24} lg={15}>
          <PageTable<SysDictItem>
            title={selectedType ? `字典项 · ${selectedType.name}` : '字典项'}
            loading={itemLoading}
            rowKey="id"
            size="small"
            extra={
              <Button type="primary" disabled={!selectedTypeId} onClick={openCreateItem}>
                新增字典项
              </Button>
            }
            columns={[
              { title: '值', dataIndex: 'itemValue', width: 140 },
              { title: '标签', dataIndex: 'itemLabel' },
              { title: '排序', dataIndex: 'sortOrder', width: 70 },
              {
                title: '状态',
                dataIndex: 'status',
                width: 90,
                render: (v: string) => (
                  <Tag color={v === 'ENABLED' ? 'success' : 'default'}>
                    {v === 'ENABLED' ? '启用' : '禁用'}
                  </Tag>
                ),
              },
              {
                title: '操作',
                width: 140,
                render: (_, row) => (
                  <Space>
                    <Button type="link" size="small" onClick={() => openEditItem(row)}>
                      编辑
                    </Button>
                    <Popconfirm title="确认删除该字典项？" onConfirm={() => void deleteItem(row.id)}>
                      <Button type="link" size="small" danger>
                        删除
                      </Button>
                    </Popconfirm>
                  </Space>
                ),
              },
            ]}
            dataSource={items}
            pagination={false}
            locale={{ emptyText: selectedTypeId ? '暂无字典项' : '请先选择左侧字典类型' }}
          />
        </Col>
      </Row>

      <Modal
        title={editingType ? '编辑字典类型' : '新增字典类型'}
        open={typeOpen}
        onOk={() => void submitType()}
        onCancel={() => setTypeOpen(false)}
        destroyOnClose
      >
        <Form form={typeForm} layout="vertical">
          <Form.Item name="code" label="编码" rules={[{ required: true }]}>
            <Input placeholder="如：common_status" disabled={!!editingType} />
          </Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="status" label="状态" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'ENABLED', label: '启用' },
                { value: 'DISABLED', label: '禁用' },
              ]}
            />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingItem ? '编辑字典项' : '新增字典项'}
        open={itemOpen}
        onOk={() => void submitItem()}
        onCancel={() => setItemOpen(false)}
        destroyOnClose
      >
        <Form form={itemForm} layout="vertical">
          <Form.Item name="typeId" hidden>
            <Input />
          </Form.Item>
          <Form.Item name="itemValue" label="字典值" rules={[{ required: true }]}>
            <Input placeholder="如：ENABLED" disabled={!!editingItem} />
          </Form.Item>
          <Form.Item name="itemLabel" label="显示标签" rules={[{ required: true }]}>
            <Input placeholder="如：启用" />
          </Form.Item>
          <Form.Item name="status" label="状态" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 'ENABLED', label: '启用' },
                { value: 'DISABLED', label: '禁用' },
              ]}
            />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber style={{ width: '100%' }} min={0} />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
