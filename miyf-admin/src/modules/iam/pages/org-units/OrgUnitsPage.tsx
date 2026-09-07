import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Form, Input, InputNumber, Popconfirm, Select, Space, message } from 'antd';
import PageTable from '@/components/PageTable';
import {
  EmptyState,
  FormItem,
  FormModal,
  PageHeader,
  PageToolbar,
  StatusBadge,
} from '@/ui';
import { iamOrgUnitApi, type IamOrgUnit } from '@/modules/iam/api';
import { useClientPager } from '@/hooks/useClientPager';
import { useSubmitting } from '@/hooks/useSubmitting';
import { ENABLED_STATUS } from '@/constants/status';
import { notifyError } from '@/api/errors';

/**
 * 组织架构：单位列表 CRUD（客户端分页）。
 */
export default function OrgUnitsPage() {
  const [loading, setLoading] = useState(false);
  const [all, setAll] = useState<IamOrgUnit[]>([]);
  const [keyword, setKeyword] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<IamOrgUnit | null>(null);
  const [form] = Form.useForm();
  const { submitting, run } = useSubmitting();

  const filtered = useMemo(() => {
    const q = keyword.trim().toLowerCase();
    if (!q) return all;
    return all.filter(
      (u) =>
        u.code.toLowerCase().includes(q) ||
        u.name.toLowerCase().includes(q),
    );
  }, [all, keyword]);

  const { data, pagination, setPage } = useClientPager(filtered);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      setAll(await iamOrgUnitApi.list());
    } catch (err) {
      setAll([]);
      notifyError(err, '加载组织架构失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const parentOptions = useMemo(
    () =>
      all
        .filter((u) => !editing || u.id !== editing.id)
        .map((u) => ({ value: u.id, label: `${u.name} (${u.code})` })),
    [all, editing],
  );

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ status: 'ENABLED', sortOrder: 0 });
    setOpen(true);
  };

  const openEdit = (row: IamOrgUnit) => {
    setEditing(row);
    form.setFieldsValue({
      code: row.code,
      name: row.name,
      parentId: row.parentId,
      sortOrder: row.sortOrder ?? 0,
      status: row.status,
    });
    setOpen(true);
  };

  const handleSubmit = () =>
    void run(async () => {
      const values = await form.validateFields();
      const payload = {
        code: values.code as string,
        name: values.name as string,
        parentId: values.parentId as string | undefined,
        sortOrder: values.sortOrder as number | undefined,
        status: values.status as string,
      };
      try {
        if (editing) {
          await iamOrgUnitApi.update(editing.id, payload);
          message.success('单位已更新');
        } else {
          await iamOrgUnitApi.create(payload);
          message.success('单位已创建');
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
        await iamOrgUnitApi.remove(id);
        message.success('已删除');
        void fetchData();
      } catch (err) {
        notifyError(err, '删除失败');
      }
    });

  return (
    <div className="ck-page">
      <PageHeader title="组织架构" description="维护组织单位层级与状态" />
      <PageToolbar
        left={
          <Input.Search
            allowClear
            placeholder="搜索编码 / 名称"
            style={{ width: 220 }}
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
          <Button type="primary" onClick={openCreate}>
            新增单位
          </Button>
        }
      />

      <PageTable<IamOrgUnit>
        title="组织单位"
        loading={loading}
        rowKey="id"
        columns={[
          { title: '编码', dataIndex: 'code' },
          { title: '名称', dataIndex: 'name' },
          {
            title: '上级',
            dataIndex: 'parentId',
            render: (v?: string) => {
              if (!v) return '—';
              const p = all.find((u) => u.id === v);
              return p?.name || v;
            },
          },
          { title: '排序', dataIndex: 'sortOrder', width: 80 },
          {
            title: '状态',
            dataIndex: 'status',
            render: (v?: string) => <StatusBadge code={v} map={ENABLED_STATUS} />,
          },
          {
            title: '操作',
            width: 160,
            render: (_, row) => (
              <Space>
                <Button type="link" size="small" onClick={() => openEdit(row)}>
                  编辑
                </Button>
                <Popconfirm title="确认删除该单位？" onConfirm={() => handleDelete(row.id)}>
                  <Button type="link" size="small" danger>
                    删除
                  </Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
        dataSource={data}
        pagination={pagination}
        locale={{ emptyText: <EmptyState description="暂无组织单位" /> }}
      />

      <FormModal
        title={editing ? '编辑单位' : '新增单位'}
        open={open}
        form={form}
        confirmLoading={submitting}
        onOk={handleSubmit}
        onCancel={() => setOpen(false)}
      >
        <FormItem name="code" label="编码" rules={[{ required: true }]}>
          <Input placeholder="如：HQ" />
        </FormItem>
        <FormItem name="name" label="名称" rules={[{ required: true }]}>
          <Input placeholder="如：总部" />
        </FormItem>
        <FormItem name="parentId" label="上级单位">
          <Select allowClear options={parentOptions} placeholder="无则留空" />
        </FormItem>
        <FormItem name="sortOrder" label="排序">
          <InputNumber style={{ width: '100%' }} min={0} />
        </FormItem>
        <FormItem name="status" label="状态" rules={[{ required: true }]}>
          <Select
            options={[
              { value: 'ENABLED', label: '启用' },
              { value: 'DISABLED', label: '停用' },
            ]}
          />
        </FormItem>
      </FormModal>
    </div>
  );
}
