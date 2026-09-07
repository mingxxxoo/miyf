import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Card, Col, Empty, Row, Segmented, Space, Table, Tag, message } from 'antd';
import ReactECharts from 'echarts-for-react';
import { useNavigate } from 'react-router-dom';
import { dashboardApi, dishApi, orderApi, notifyError } from '@/api';
import type { DashboardStats, Order, OrderStatus } from '@/types';
import { ORDER_STATUS, statusOf } from '@/constants/status';
import {
  EmptyState,
  ErrorState,
  LoadingState,
  MetricCard,
  PageHeader,
  StatusBadge,
} from '@/ui';
import { useAuthStore } from '@/stores/authStore';

const emptyStats: DashboardStats = {
  userCount: 0,
  todayOrders: 0,
  dishCount: 0,
  pendingComments: 0,
  reservationTrend: [],
  hotDishes: [],
  ratingDistribution: [],
  userActivity: [],
};

type RangeKey = '7d' | '14d' | '30d';

function takeLast<T>(list: T[], n: number): T[] {
  if (!list.length) return [];
  return list.slice(Math.max(0, list.length - n));
}

export default function DashboardPage() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<DashboardStats>(emptyStats);
  const [error, setError] = useState<unknown>(null);
  const [range, setRange] = useState<RangeKey>('7d');
  const [pendingOrders, setPendingOrders] = useState<Order[]>([]);
  const [onSaleCount, setOnSaleCount] = useState(0);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [data, pending, dishes] = await Promise.all([
        dashboardApi.stats(),
        orderApi.page({ page: 1, pageSize: 5, status: 'PENDING' }),
        dishApi.page({ page: 1, pageSize: 1, status: 'ON_SALE' }),
      ]);
      setStats(data);
      setPendingOrders(pending.records ?? []);
      setOnSaleCount(dishes.total ?? data.dishCount ?? 0);
    } catch (err) {
      setStats(emptyStats);
      setPendingOrders([]);
      setError(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const days = range === '7d' ? 7 : range === '14d' ? 14 : 30;
  const trend = useMemo(() => takeLast(stats.reservationTrend, days), [stats.reservationTrend, days]);
  const activity = useMemo(() => takeLast(stats.userActivity, days), [stats.userActivity, days]);

  const greeting = useMemo(() => {
    const name = user?.nickname || user?.username || '管理员';
    const hour = new Date().getHours();
    const hello = hour < 12 ? '早上好' : hour < 18 ? '下午好' : '晚上好';
    return `${hello}，${name}`;
  }, [user]);

  const confirmPending = async (order: Order) => {
    try {
      await orderApi.updateStatus(order.id, 'CONFIRMED' as OrderStatus);
      message.success(`已确认预约 ${order.orderNo}`);
      void load();
    } catch (err) {
      notifyError(err, '确认失败');
    }
  };

  const reservationTrendOption = {
    color: ['#FFB36B'],
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: trend.length ? trend.map((d) => d.date) : ['暂无'],
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '预约数',
        type: 'line',
        smooth: true,
        areaStyle: { color: 'rgba(255, 179, 107, 0.18)' },
        data: trend.length ? trend.map((d) => d.count) : [0],
      },
    ],
  };

  const hotDishesOption = {
    color: ['#9DD9C4'],
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: {
      type: 'category',
      data: stats.hotDishes.length ? stats.hotDishes.map((d) => d.name) : ['暂无数据'],
    },
    series: [
      {
        name: '预约人次',
        type: 'bar',
        data: stats.hotDishes.length ? stats.hotDishes.map((d) => d.count) : [0],
      },
    ],
  };

  const userActivityOption = {
    color: ['#9DD9C4'],
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: activity.length ? activity.map((d) => d.date) : ['暂无'],
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '活跃用户',
        type: 'bar',
        data: activity.length ? activity.map((d) => d.activeUsers) : [0],
      },
    ],
  };

  if (error && !loading) {
    return (
      <div className="ck-page">
        <PageHeader title="数据概览" description="共享厨房运营一眼看清" />
        <ErrorState error={error} onRetry={() => void load()} />
      </div>
    );
  }

  return (
    <div className="ck-page">
      <PageHeader
        title="数据概览"
        description={`${greeting} · 今天厨房还有 ${pendingOrders.length} 单待确认`}
        extra={
          <Space wrap>
            <Button onClick={() => navigate('/kitchen/dishes/create')}>新增菜品</Button>
            <Button type="primary" onClick={() => navigate('/kitchen/orders')}>
              处理预约
            </Button>
          </Space>
        }
      />

      <LoadingState spinning={loading}>
        <Row gutter={[16, 16]} style={{ marginBottom: 20 }}>
          <Col xs={24} sm={12} lg={6}>
            <MetricCard
              label="今日预约"
              value={stats.todayOrders}
              hint="含所有状态"
              onClick={() => navigate('/kitchen/orders')}
            />
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <MetricCard
              label="待确认"
              value={pendingOrders.length || '—'}
              hint="需尽快处理"
              onClick={() => navigate('/kitchen/orders')}
            />
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <MetricCard
              label="上架菜品"
              value={onSaleCount || stats.dishCount}
              onClick={() => navigate('/kitchen/dishes')}
            />
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <MetricCard
              label="隐藏评论"
              value={stats.pendingComments}
              onClick={() => navigate('/kitchen/comments')}
            />
          </Col>
        </Row>

        <Card
          bordered={false}
          title="待处理预约"
          extra={<Button type="link" onClick={() => navigate('/kitchen/orders')}>查看全部</Button>}
          style={{ marginBottom: 16 }}
        >
          {pendingOrders.length === 0 ? (
            <EmptyState
              description="暂时没有待确认预约，去上架几道菜吧"
              actionText="管理菜品"
              onAction={() => navigate('/kitchen/dishes')}
            />
          ) : (
            <Table<Order>
              rowKey="id"
              size="small"
              pagination={false}
              dataSource={pendingOrders}
              columns={[
                { title: '单号', dataIndex: 'orderNo' },
                { title: '用户', dataIndex: 'userNickname', render: (v?: string) => v || '—' },
                {
                  title: '菜品',
                  render: (_, r) => r.items?.map((i) => i.dishName).join('、') || '—',
                },
                {
                  title: '状态',
                  dataIndex: 'status',
                  render: (s: string) => <StatusBadge code={s} map={ORDER_STATUS} />,
                },
                {
                  title: '操作',
                  width: 180,
                  render: (_, row) => (
                    <Space>
                      <Button type="link" size="small" onClick={() => void confirmPending(row)}>
                        快速确认
                      </Button>
                      <Button
                        type="link"
                        size="small"
                        onClick={() => navigate(`/kitchen/orders/${row.id}`)}
                      >
                        详情
                      </Button>
                    </Space>
                  ),
                },
              ]}
            />
          )}
        </Card>

        <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 12 }}>
          <Segmented
            value={range}
            onChange={(v) => setRange(v as RangeKey)}
            options={[
              { label: '近 7 日', value: '7d' },
              { label: '近 14 日', value: '14d' },
              { label: '近 30 日', value: '30d' },
            ]}
          />
        </div>

        {!loading && trend.length === 0 && stats.hotDishes.length === 0 ? (
          <Empty
            description="暂无统计数据，厨房还在安静地等待第一批预约"
            style={{ marginBottom: 24 }}
          >
            <Button type="primary" onClick={() => navigate('/kitchen/dishes/create')}>
              发布第一道菜
            </Button>
          </Empty>
        ) : null}

        <Row gutter={[16, 16]}>
          <Col xs={24} lg={12}>
            <Card title="预约趋势" bordered={false} className="ck-chart-card">
              <ReactECharts option={reservationTrendOption} style={{ height: 300 }} />
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card title="热门菜品" bordered={false} className="ck-chart-card">
              <ReactECharts option={hotDishesOption} style={{ height: 300 }} />
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card title="用户活跃度" bordered={false} className="ck-chart-card">
              <ReactECharts option={userActivityOption} style={{ height: 300 }} />
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card title="快捷入口" bordered={false}>
              <Space wrap>
                <Tag
                  color="orange"
                  style={{ cursor: 'pointer', padding: '6px 12px' }}
                  onClick={() => navigate('/kitchen/orders')}
                >
                  预约管理 · {statusOf(ORDER_STATUS, 'PENDING').text}
                </Tag>
                <Tag
                  color="green"
                  style={{ cursor: 'pointer', padding: '6px 12px' }}
                  onClick={() => navigate('/kitchen/dishes')}
                >
                  菜品上架
                </Tag>
                <Tag
                  style={{ cursor: 'pointer', padding: '6px 12px' }}
                  onClick={() => navigate('/kitchen/comments')}
                >
                  评论审核
                </Tag>
                <Tag
                  style={{ cursor: 'pointer', padding: '6px 12px' }}
                  onClick={() => navigate('/kitchen/users')}
                >
                  用户一览
                </Tag>
              </Space>
            </Card>
          </Col>
        </Row>
      </LoadingState>
    </div>
  );
}
