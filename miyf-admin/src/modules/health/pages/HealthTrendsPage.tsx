import { useCallback, useEffect, useMemo, useState } from 'react';
import { Card, Col, Empty, Row, Select, Space, Statistic, Typography, message } from 'antd';
import ReactECharts from 'echarts-for-react';
import { healthApi, type HealthSubject, type HealthTrend } from '@/modules/health/api';

const METRICS = [
  { value: 'WEIGHT', label: '体重' },
  { value: 'HEART_RATE', label: '心率' },
  { value: 'BMI', label: 'BMI' },
  { value: 'BODY_FAT', label: '体脂' },
  { value: 'STEPS', label: '步数' },
  { value: 'SLEEP_MINUTES', label: '睡眠(分)' },
  { value: 'BLOOD_PRESSURE_SYS', label: '收缩压' },
  { value: 'BLOOD_PRESSURE_DIA', label: '舒张压' },
  { value: 'BLOOD_GLUCOSE', label: '血糖' },
];

/**
 * 健康指标趋势图。
 */
export default function HealthTrendsPage() {
  const [subjects, setSubjects] = useState<HealthSubject[]>([]);
  const [subjectId, setSubjectId] = useState<string | undefined>();
  const [metricCode, setMetricCode] = useState('WEIGHT');
  const [loading, setLoading] = useState(false);
  const [trend, setTrend] = useState<HealthTrend | null>(null);

  useEffect(() => {
    void (async () => {
      try {
        const list = await healthApi.listSubjects();
        setSubjects(list);
        if (list.length && !subjectId) {
          setSubjectId(list[0].id);
        }
      } catch (err) {
        message.error(err instanceof Error ? err.message : '加载主体失败');
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const fetchTrend = useCallback(async () => {
    if (!subjectId || !metricCode) {
      setTrend(null);
      return;
    }
    setLoading(true);
    try {
      setTrend(await healthApi.trend({ subjectId, metricCode, limit: 365 }));
    } catch (err) {
      setTrend(null);
      message.error(err instanceof Error ? err.message : '加载趋势失败');
    } finally {
      setLoading(false);
    }
  }, [subjectId, metricCode]);

  useEffect(() => {
    void fetchTrend();
  }, [fetchTrend]);

  const chartOption = useMemo(() => {
    const points = trend?.points ?? [];
    const dates = points.map((p) =>
      p.measuredAt ? new Date(p.measuredAt).toLocaleString() : '',
    );
    const values = points.map((p) => p.value ?? null);
    const metricLabel = METRICS.find((m) => m.value === metricCode)?.label || metricCode;
    return {
      tooltip: { trigger: 'axis' },
      grid: { left: 48, right: 24, top: 40, bottom: 48 },
      xAxis: {
        type: 'category',
        data: dates,
        axisLabel: { rotate: dates.length > 12 ? 35 : 0 },
      },
      yAxis: {
        type: 'value',
        name: trend?.unit || '',
        scale: true,
      },
      series: [
        {
          name: metricLabel,
          type: 'line',
          smooth: true,
          showSymbol: points.length < 40,
          data: values,
          areaStyle: { opacity: 0.08 },
        },
      ],
    };
  }, [trend, metricCode]);

  return (
    <div className="ck-page">
      <h2 className="ck-page-title">健康趋势</h2>
      <Typography.Paragraph type="secondary">
        按主体与指标查看时间序列，支持体重、心率等规范编码。
      </Typography.Paragraph>
      <Space style={{ marginBottom: 16 }} wrap>
        <Select
          placeholder="主体"
          style={{ width: 200 }}
          value={subjectId}
          onChange={setSubjectId}
          options={subjects.map((s) => ({ value: s.id, label: s.displayName }))}
        />
        <Select
          style={{ width: 160 }}
          value={metricCode}
          onChange={setMetricCode}
          options={METRICS}
        />
      </Space>

      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={12} md={6}>
          <Card loading={loading}>
            <Statistic title="最新值" value={trend?.latest ?? '-'} suffix={trend?.unit} />
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card loading={loading}>
            <Statistic title="平均" value={trend?.avg ?? '-'} suffix={trend?.unit} />
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card loading={loading}>
            <Statistic title="最低" value={trend?.min ?? '-'} suffix={trend?.unit} />
          </Card>
        </Col>
        <Col xs={12} md={6}>
          <Card loading={loading}>
            <Statistic title="最高" value={trend?.max ?? '-'} suffix={trend?.unit} />
          </Card>
        </Col>
      </Row>

      <Card title="趋势图" loading={loading}>
        {!trend?.points?.length ? (
          <Empty description="暂无采样点，请先在「采样」页录入或同步数据" />
        ) : (
          <ReactECharts option={chartOption} style={{ height: 360 }} notMerge />
        )}
      </Card>
    </div>
  );
}
