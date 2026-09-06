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
import { healthApi, type HealthSample, type HealthSubject } from '@/modules/health/api';

const METRICS = [
  { value: 'WEIGHT', label: '体重 WEIGHT' },
  { value: 'HEIGHT', label: '身高 HEIGHT' },
  { value: 'BMI', label: 'BMI' },
  { value: 'BODY_FAT', label: '体脂 BODY_FAT' },
  { value: 'HEART_RATE', label: '心率 HEART_RATE' },
  { value: 'STEPS', label: '步数 STEPS' },
  { value: 'SLEEP_MINUTES', label: '睡眠分钟 SLEEP_MINUTES' },
  { value: 'BLOOD_PRESSURE_SYS', label: '收缩压' },
  { value: 'BLOOD_PRESSURE_DIA', label: '舒张压' },
  { value: 'BLOOD_GLUCOSE', label: '血糖' },
];

function toLocalInputValue(d = new Date()) {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function toIso(local: string) {
  const t = Date.parse(local);
  return Number.isNaN(t) ? new Date().toISOString() : new Date(t).toISOString();
}

/**
 * 健康采样列表与手动录入。
 */
export default function HealthSamplesPage() {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<HealthSample[]>([]);
  const [subjects, setSubjects] = useState<HealthSubject[]>([]);
  const [subjectId, setSubjectId] = useState<string | undefined>();
  const [metricCode, setMetricCode] = useState<string | undefined>();
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm();

  const fetchSubjects = useCallback(async () => {
    try {
      setSubjects(await healthApi.listSubjects());
    } catch {
      setSubjects([]);
    }
  }, []);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      setRows(
        await healthApi.listSamples({
          subjectId,
          metricCode,
          limit: 100,
        }),
      );
    } catch (err) {
      setRows([]);
      message.error(err instanceof Error ? err.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, [subjectId, metricCode]);

  useEffect(() => {
    void fetchSubjects();
  }, [fetchSubjects]);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const onSave = async () => {
    const values = await form.validateFields();
    try {
      await healthApi.createSample({
        subjectId: values.subjectId,
        metricCode: values.metricCode,
        valueNum: values.valueNum,
        unit: values.unit,
        measuredAt: toIso(values.measuredAt),
        quality: values.quality || 'NORMAL',
      });
      message.success('已录入');
      setOpen(false);
      form.resetFields();
      await fetchData();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '录入失败');
    }
  };

  const onRemove = async (id: string) => {
    try {
      await healthApi.removeSample(id);
      message.success('已删除');
      await fetchData();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '删除失败');
    }
  };

  const subjectName = (id: string) =>
    subjects.find((s) => s.id === id)?.displayName || id;

  return (
    <div className="ck-page">
      <h2 className="ck-page-title">健康采样</h2>
      <Space style={{ marginBottom: 16 }} wrap>
        <Select
          allowClear
          placeholder="主体"
          style={{ width: 180 }}
          value={subjectId}
          onChange={setSubjectId}
          options={subjects.map((s) => ({ value: s.id, label: s.displayName }))}
        />
        <Select
          allowClear
          placeholder="指标"
          style={{ width: 200 }}
          value={metricCode}
          onChange={setMetricCode}
          options={METRICS}
        />
        <Button onClick={() => void fetchData()}>查询</Button>
        <Button
          type="primary"
          onClick={() => {
            form.setFieldsValue({
              subjectId,
              measuredAt: toLocalInputValue(),
              quality: 'NORMAL',
            });
            setOpen(true);
          }}
        >
          手动录入
        </Button>
      </Space>
      <Table
        rowKey="id"
        loading={loading}
        dataSource={rows}
        pagination={{ pageSize: 10 }}
        columns={[
          {
            title: '主体',
            dataIndex: 'subjectId',
            render: (id: string) => subjectName(id),
          },
          { title: '指标', dataIndex: 'metricCode', width: 160 },
          {
            title: '数值',
            dataIndex: 'valueNum',
            width: 100,
            render: (v: number, row) => `${v ?? '-'}${row.unit ? ` ${row.unit}` : ''}`,
          },
          { title: '数据源', dataIndex: 'providerCode', width: 100 },
          { title: '质量', dataIndex: 'quality', width: 100 },
          { title: '测量时间', dataIndex: 'measuredAt', width: 200 },
          {
            title: '操作',
            width: 90,
            render: (_, row) => (
              <Popconfirm title="确认删除？" onConfirm={() => void onRemove(row.id)}>
                <Button type="link" size="small" danger>
                  删除
                </Button>
              </Popconfirm>
            ),
          },
        ]}
      />
      <Modal
        title="手动录入采样"
        open={open}
        onCancel={() => setOpen(false)}
        onOk={() => void onSave()}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="subjectId" label="主体" rules={[{ required: true }]}>
            <Select options={subjects.map((s) => ({ value: s.id, label: s.displayName }))} />
          </Form.Item>
          <Form.Item name="metricCode" label="指标" rules={[{ required: true }]}>
            <Select options={METRICS} />
          </Form.Item>
          <Form.Item name="valueNum" label="数值" rules={[{ required: true }]}>
            <InputNumber style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="unit" label="单位">
            <Input placeholder="空则使用默认单位" />
          </Form.Item>
          <Form.Item name="measuredAt" label="测量时间" rules={[{ required: true }]}>
            <Input placeholder="YYYY-MM-DDTHH:mm" />
          </Form.Item>
          <Form.Item name="quality" label="质量">
            <Select
              options={[
                { value: 'NORMAL', label: 'NORMAL' },
                { value: 'ESTIMATED', label: 'ESTIMATED' },
                { value: 'SUSPECT', label: 'SUSPECT' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
