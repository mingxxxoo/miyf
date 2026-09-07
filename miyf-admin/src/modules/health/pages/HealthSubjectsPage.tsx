import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  message,
} from 'antd';
import { healthApi, type HealthSubject } from '@/modules/health/api';
import { PageHeader, StatusBadge } from '@/ui';
import { ENABLED_STATUS } from '@/constants/status';

/**
 * 健康主体 CRUD。
 */
export default function HealthSubjectsPage() {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<HealthSubject[]>([]);
  const [keyword, setKeyword] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<HealthSubject | null>(null);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      setRows(await healthApi.listSubjects(keyword.trim() || undefined));
    } catch (err) {
      setRows([]);
      message.error(err instanceof Error ? err.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, [keyword]);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const openCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({ gender: 'UNKNOWN', status: 'ENABLED' });
    setOpen(true);
  };

  const openEdit = (row: HealthSubject) => {
    setEditing(row);
    form.setFieldsValue({
      displayName: row.displayName,
      gender: row.gender || 'UNKNOWN',
      birthDate: row.birthDate,
      heightCm: row.heightCm,
      status: row.status || 'ENABLED',
      remark: row.remark,
    });
    setOpen(true);
  };

  const onSave = async () => {
    const values = await form.validateFields();
    try {
      if (editing) {
        await healthApi.updateSubject(editing.id, values);
      } else {
        await healthApi.createSubject(values);
      }
      message.success('已保存');
      setOpen(false);
      await fetchData();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '保存失败');
    }
  };

  const onRemove = async (id: string) => {
    try {
      await healthApi.removeSubject(id);
      message.success('已删除');
      await fetchData();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '删除失败');
    }
  };

  return (
    <div className="ck-page">
      <PageHeader title="健康主体" />
<Space style={{ marginBottom: 16 }} wrap>
        <Input
          allowClear
          placeholder="搜索姓名"
          style={{ width: 200 }}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          onPressEnter={() => void fetchData()}
        />
        <Button onClick={() => void fetchData()}>查询</Button>
        <Button type="primary" onClick={openCreate}>
          新建
        </Button>
      </Space>
      <Table
        rowKey="id"
        loading={loading}
        dataSource={rows}
        pagination={{ pageSize: 10 }}
        columns={[
          { title: '姓名', dataIndex: 'displayName' },
          { title: '性别', dataIndex: 'gender', width: 100 },
          { title: '出生日期', dataIndex: 'birthDate', width: 130 },
          { title: '身高(cm)', dataIndex: 'heightCm', width: 100 },
          {
            title: '状态',
            dataIndex: 'status',
            width: 100,
            render: (v: string) => <StatusBadge code={v} map={ENABLED_STATUS} />,
          },
          { title: '备注', dataIndex: 'remark', ellipsis: true },
          {
            title: '操作',
            width: 160,
            render: (_, row) => (
              <Space>
                <Button type="link" size="small" onClick={() => openEdit(row)}>
                  编辑
                </Button>
                <Popconfirm title="确认删除？" onConfirm={() => void onRemove(row.id)}>
                  <Button type="link" size="small" danger>
                    删除
                  </Button>
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />
      <Modal
        title={editing ? '编辑主体' : '新建主体'}
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => void onSave()}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="displayName" label="姓名" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="gender" label="性别">
            <Select
              options={[
                { value: 'UNKNOWN', label: '未知' },
                { value: 'MALE', label: '男' },
                { value: 'FEMALE', label: '女' },
              ]}
            />
          </Form.Item>
          <Form.Item name="birthDate" label="出生日期">
            <Input placeholder="YYYY-MM-DD" />
          </Form.Item>
          <Form.Item name="heightCm" label="身高(cm)">
            <InputNumber style={{ width: '100%' }} min={0} max={300} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select
              options={[
                { value: 'ENABLED', label: '启用' },
                { value: 'DISABLED', label: '停用' },
              ]}
            />
          </Form.Item>
          <Form.Item name="remark" label="备注">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
