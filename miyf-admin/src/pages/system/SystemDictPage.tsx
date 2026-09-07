import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button,
  Form,
  Input,
  InputNumber,
  List,
  Popconfirm,
  Select,
  Space,
  Table,
  Tooltip,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { notifyError } from '@/api/errors';
import { ENABLED_STATUS } from '@/constants/status';
import { useSubmitting } from '@/hooks/useSubmitting';
import { sysDictApi, type SysDictItem, type SysDictType } from '@/modules/system/api';
import {
  EmptyState,
  FormItem,
  FormModal,
  PageHeader,
  SplitWorkspace,
  StatusBadge,
} from '@/ui';

/**
 * 数据字典：左侧类型 / 右侧字典项。启用中的项禁止删除。
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
  const { submitting, run } = useSubmitting();

  const selectedType = types.find((t) => t.id === selectedTypeId) ?? null;

  const sortedItems = useMemo(
    () =>
      [...items].sort(
        (a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.itemValue.localeCompare(b.itemValue),
      ),
    [items],
  );

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
      notifyError(err, '加载字典类型失败');
    } finally {
      setTypeLoading(false);
    }
  }, [typeKeyword]);

  const fetchItems = useCallback(async (typeId: string) => {
    setItemLoading(true);
    try {
      setItems(await sysDictApi.listItems(typeId));
    } catch (err) {
      setItems([]);
      notifyError(err, '加载字典项失败');
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

  const submitType = () =>
    run(async () => {
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
        await fetchTypes();
      } catch (err) {
        notifyError(err, '保存失败');
      }
    });

  const deleteType = (id: string) =>
    run(async () => {
      try {
        await sysDictApi.removeType(id);
        message.success('已删除');
        if (selectedTypeId === id) setSelectedTypeId(null);
        await fetchTypes();
      } catch (err) {
        notifyError(err, '删除失败');
      }
    });

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

  const submitItem = () =>
    run(async () => {
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
        if (selectedTypeId) await fetchItems(selectedTypeId);
      } catch (err) {
        notifyError(err, '保存失败');
      }
    });

  const deleteItem = (row: SysDictItem) =>
    run(async () => {
      if (row.status === 'ENABLED') {
        message.warning('使用中的字典项请先停用');
        return;
      }
      try {
        await sysDictApi.removeItem(row.id);
        message.success('已删除');
        if (selectedTypeId) await fetchItems(selectedTypeId);
      } catch (err) {
        notifyError(err, '删除失败');
      }
    });

  const itemColumns: ColumnsType<SysDictItem> = [
    { title: '值', dataIndex: 'itemValue', width: 140 },
    { title: '标签', dataIndex: 'itemLabel' },
    { title: '排序', dataIndex: 'sortOrder', width: 80 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (v: string) => <StatusBadge code={v} map={ENABLED_STATUS} />,
    },
    {
      title: '操作',
      width: 150,
      render: (_, row) => {
        const enabled = row.status === 'ENABLED';
        const deleteBtn = (
          <Button type="link" size="small" danger disabled={enabled}>
            删除
          </Button>
        );
        return (
          <Space>
            <Button type="link" size="small" onClick={() => openEditItem(row)}>
              编辑
            </Button>
            {enabled ? (
              <Tooltip title="使用中的字典项请先停用">{deleteBtn}</Tooltip>
            ) : (
              <Popconfirm title="确认删除该字典项？" onConfirm={() => void deleteItem(row)}>
                {deleteBtn}
              </Popconfirm>
            )}
          </Space>
        );
      },
    },
  ];

  return (
    <div className="ck-page">
      <PageHeader
        title="数据字典"
        description="维护字典类型与字典项；使用中的字典项需先停用再删除"
      />

      <SplitWorkspace
        leftWidth={360}
        leftTitle="字典类型"
        leftExtra={
          <Space wrap>
            <Input.Search
              allowClear
              placeholder="编码 / 名称"
              style={{ width: 140 }}
              onSearch={setTypeKeyword}
            />
            <Button type="primary" size="small" onClick={openCreateType}>
              新增
            </Button>
          </Space>
        }
        left={
          types.length ? (
            <List
              loading={typeLoading}
              dataSource={types}
              renderItem={(row) => (
                <List.Item
                  className={row.id === selectedTypeId ? 'ant-list-item-selected' : undefined}
                  style={{
                    cursor: 'pointer',
                    padding: '10px 16px',
                    background: row.id === selectedTypeId ? 'var(--ck-primary-soft, #fff7ed)' : undefined,
                  }}
                  onClick={() => setSelectedTypeId(row.id)}
                  actions={[
                    <Button key="edit" type="link" size="small" onClick={(e) => {
                      e.stopPropagation();
                      openEditType(row);
                    }}>
                      编辑
                    </Button>,
                    <Popconfirm
                      key="del"
                      title="删除类型将级联删除字典项，确认？"
                      onConfirm={() => void deleteType(row.id)}
                    >
                      <Button type="link" size="small" danger onClick={(e) => e.stopPropagation()}>
                        删
                      </Button>
                    </Popconfirm>,
                  ]}
                >
                  <List.Item.Meta
                    title={
                      <Space>
                        <span>{row.name}</span>
                        <StatusBadge code={row.status} map={ENABLED_STATUS} />
                      </Space>
                    }
                    description={
                      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                        {row.code}
                        {row.description ? ` · ${row.description}` : ''}
                      </Typography.Text>
                    }
                  />
                </List.Item>
              )}
            />
          ) : (
            <EmptyState description="暂无字典类型" actionText="新增类型" onAction={openCreateType} />
          )
        }
        rightTitle={selectedType ? `字典项 · ${selectedType.name}` : '字典项'}
        rightExtra={
          <Button type="primary" disabled={!selectedTypeId} onClick={openCreateItem}>
            新增字典项
          </Button>
        }
        right={
          <Table<SysDictItem>
            rowKey="id"
            size="small"
            loading={itemLoading}
            columns={itemColumns}
            dataSource={sortedItems}
            pagination={false}
            locale={{
              emptyText: (
                <EmptyState
                  description={selectedTypeId ? '暂无字典项' : '请先选择左侧字典类型'}
                />
              ),
            }}
          />
        }
      />

      <FormModal
        title={editingType ? '编辑字典类型' : '新增字典类型'}
        open={typeOpen}
        form={typeForm}
        confirmLoading={submitting}
        onOk={() => void submitType()}
        onCancel={() => setTypeOpen(false)}
      >
        <FormItem name="code" label="编码" rules={[{ required: true }]}>
          <Input placeholder="如：common_status" disabled={!!editingType} />
        </FormItem>
        <FormItem name="name" label="名称" rules={[{ required: true }]}>
          <Input />
        </FormItem>
        <FormItem name="status" label="状态" rules={[{ required: true }]}>
          <Select
            options={[
              { value: 'ENABLED', label: '启用' },
              { value: 'DISABLED', label: '禁用' },
            ]}
          />
        </FormItem>
        <FormItem name="sortOrder" label="排序">
          <InputNumber style={{ width: '100%' }} min={0} />
        </FormItem>
        <FormItem name="description" label="说明" full>
          <Input.TextArea rows={2} />
        </FormItem>
      </FormModal>

      <FormModal
        title={editingItem ? '编辑字典项' : '新增字典项'}
        open={itemOpen}
        form={itemForm}
        confirmLoading={submitting}
        onOk={() => void submitItem()}
        onCancel={() => setItemOpen(false)}
      >
        <FormItem name="typeId" hidden>
          <Input />
        </FormItem>
        <FormItem name="itemValue" label="字典值" rules={[{ required: true }]}>
          <Input placeholder="如：ENABLED" disabled={!!editingItem} />
        </FormItem>
        <FormItem name="itemLabel" label="显示标签" rules={[{ required: true }]}>
          <Input placeholder="如：启用" />
        </FormItem>
        <FormItem name="status" label="状态" rules={[{ required: true }]}>
          <Select
            options={[
              { value: 'ENABLED', label: '启用' },
              { value: 'DISABLED', label: '禁用' },
            ]}
          />
        </FormItem>
        <FormItem name="sortOrder" label="排序">
          <InputNumber style={{ width: '100%' }} min={0} />
        </FormItem>
        <FormItem name="remark" label="备注" full>
          <Input.TextArea rows={2} />
        </FormItem>
      </FormModal>
    </div>
  );
}
