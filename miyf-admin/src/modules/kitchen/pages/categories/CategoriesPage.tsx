import { useCallback, useEffect, useState } from 'react';

import { Input, InputNumber, Switch, Button, Space, message, Modal, Form } from 'antd';

import SearchForm from '@/components/SearchForm';

import PageTable from '@/components/PageTable';

import { confirmDialog } from '@/components/ConfirmDialog';

import { categoryApi } from '@/api';

import type { Category } from '@/types';
import { EmptyState, PageHeader, StatusBadge } from '@/ui';
import { ENABLED_STATUS } from '@/constants/status';



export default function CategoriesPage() {

  const [loading, setLoading] = useState(false);

  const [data, setData] = useState<Category[]>([]);

  const [total, setTotal] = useState(0);

  const [page, setPage] = useState(1);

  const [pageSize, setPageSize] = useState(10);

  const [keyword, setKeyword] = useState('');

  const [modalOpen, setModalOpen] = useState(false);

  const [editing, setEditing] = useState<Category | null>(null);

  const [form] = Form.useForm();



  const fetchData = useCallback(async () => {

    setLoading(true);

    try {

      const res = await categoryApi.page({ page, pageSize, keyword: keyword || undefined });

      setData(res.records ?? []);

      setTotal(res.total ?? 0);

    } catch (err) {

      setData([]);

      setTotal(0);

      message.error(err instanceof Error ? err.message : '加载分类失败');

    } finally {

      setLoading(false);

    }

  }, [page, pageSize, keyword]);



  useEffect(() => {

    void fetchData();

  }, [fetchData]);



  const openCreate = () => {

    setEditing(null);

    form.resetFields();

    form.setFieldsValue({ sortOrder: 0, enabled: true });

    setModalOpen(true);

  };



  const openEdit = (record: Category) => {

    setEditing(record);

    form.setFieldsValue({

      ...record,

      enabled: record.status === 'ENABLED' || record.enabled !== false,

    });

    setModalOpen(true);

  };



  const handleSubmit = async () => {

    const values = await form.validateFields();

    try {

      const payload = {

        name: values.name as string,

        sortOrder: Number(values.sortOrder ?? 0),

        status: values.enabled ? 'ENABLED' : 'DISABLED',

        enabled: Boolean(values.enabled),

      };

      if (editing) {

        await categoryApi.update(editing.id, payload);

        message.success('分类已更新');

      } else {

        await categoryApi.create(payload);

        message.success('分类已创建');

      }

      setModalOpen(false);

      void fetchData();

    } catch (err) {

      message.error(err instanceof Error ? err.message : '保存失败');

    }

  };



  const handleDelete = (record: Category) => {

    confirmDialog({

      title: '删除分类',

      content: `确定删除「${record.name}」吗？`,

      danger: true,

      onOk: async () => {

        try {

          await categoryApi.remove(record.id);

          message.success('已删除');

          void fetchData();

        } catch (err) {

          message.error(err instanceof Error ? err.message : '删除失败');

        }

      },

    });

  };



  return (

    <div className="ck-page">

      <PageHeader title="分类管理" />
<SearchForm

        fields={[

          { name: 'keyword', label: '分类名称', element: <Input placeholder="搜索分类" allowClear /> },

        ]}

        loading={loading}

        onSearch={(v) => {

          setKeyword(String(v.keyword ?? ''));

          setPage(1);

        }}

      />

      <PageTable<Category>

        title="分类列表"

        loading={loading}

        rowKey="id"

        extra={

          <Button type="primary" onClick={openCreate}>

            新增分类

          </Button>

        }

        columns={[

          { title: '名称', dataIndex: 'name' },

          { title: '排序', dataIndex: 'sortOrder', width: 80 },

          {

            title: '状态',

            dataIndex: 'status',

            render: (status: string) => <StatusBadge code={status} map={ENABLED_STATUS} />,

          },

          {

            title: '操作',

            key: 'action',

            render: (_, record) => (

              <Space>

                <Button type="link" onClick={() => openEdit(record)}>

                  编辑

                </Button>

                <Button type="link" danger onClick={() => handleDelete(record)}>

                  删除

                </Button>

              </Space>

            ),

          },

        ]}

        dataSource={data}

        pagination={{

          current: page,

          pageSize,

          total,

          onChange: (p, ps) => {

            setPage(p);

            setPageSize(ps);

          },

        }}

        locale={{ emptyText: <EmptyState description="还没有分类，先建一个吧" /> }}

      />



      <Modal

        title={editing ? '编辑分类' : '新增分类'}

        open={modalOpen}

        onOk={() => void handleSubmit()}

        onCancel={() => setModalOpen(false)}

        destroyOnClose

      >

        <Form form={form} layout="vertical">

          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>

            <Input placeholder="如：家常菜" />

          </Form.Item>

          <Form.Item name="sortOrder" label="排序">

            <InputNumber min={0} style={{ width: '100%' }} />

          </Form.Item>

          <Form.Item name="enabled" label="启用" valuePropName="checked">

            <Switch />

          </Form.Item>

        </Form>

      </Modal>

    </div>

  );

}


