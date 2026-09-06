import { useEffect, useState } from 'react';
import { Row, Col, Card, Empty, Spin } from 'antd';
import ReactECharts from 'echarts-for-react';
import { dashboardApi } from '@/api';
import type { DashboardStats } from '@/types';

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

export default function DashboardPage() {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<DashboardStats>(emptyStats);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await dashboardApi.stats();
        if (!cancelled) setStats(data);
      } catch (err) {
        if (!cancelled) {
          setStats(emptyStats);
          setError(err instanceof Error ? err.message : '仪表盘数据加载失败');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const trendDates =
    stats.reservationTrend.length > 0
      ? stats.reservationTrend.map((d) => d.date)
      : ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
  const trendValues =
    stats.reservationTrend.length > 0
      ? stats.reservationTrend.map((d) => d.count)
      : [0, 0, 0, 0, 0, 0, 0];

  const reservationTrendOption = {
    color: ['#FFB36B'],
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: trendDates },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '预约数',
        type: 'line',
        smooth: true,
        areaStyle: { color: 'rgba(255, 179, 107, 0.18)' },
        data: trendValues,
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
      data:
        stats.hotDishes.length > 0
          ? stats.hotDishes.map((d) => d.name)
          : ['暂无数据'],
    },
    series: [
      {
        name: '预约人次',
        type: 'bar',
        data: stats.hotDishes.length > 0 ? stats.hotDishes.map((d) => d.count) : [0],
      },
    ],
  };

  const ratingDistOption = {
    color: ['#FFB36B', '#FFC98A', '#FFD9A8', '#9DD9C4', '#7BC9AD'],
    tooltip: { trigger: 'item' },
    series: [
      {
        name: '评分分布',
        type: 'pie',
        radius: ['40%', '70%'],
        data:
          stats.ratingDistribution.length > 0
            ? stats.ratingDistribution.map((d) => ({
                name: `${d.rating} 星`,
                value: d.count,
              }))
            : [{ name: '暂无评分', value: 0 }],
      },
    ],
  };

  const activityDates =
    stats.userActivity.length > 0
      ? stats.userActivity.map((d) => d.date)
      : trendDates;
  const activityValues =
    stats.userActivity.length > 0
      ? stats.userActivity.map((d) => d.activeUsers)
      : [0, 0, 0, 0, 0, 0, 0];

  const userActivityOption = {
    color: ['#9DD9C4'],
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: activityDates },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '活跃用户',
        type: 'bar',
        data: activityValues,
      },
    ],
  };

  const cards = [
    { label: '注册用户', value: stats.userCount },
    { label: '今日预约', value: stats.todayOrders },
    { label: '上架菜品', value: stats.dishCount },
    { label: '隐藏评论', value: stats.pendingComments },
  ];

  return (
    <div className="ck-page">
      <h2 className="ck-page-title">数据概览</h2>
      {error && (
        <Empty description={error} style={{ marginBottom: 16 }} />
      )}
      <Spin spinning={loading}>
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          {cards.map((stat) => (
            <Col xs={24} sm={12} lg={6} key={stat.label}>
              <div className="stat-card">
                <div className="stat-card-value">{stat.value}</div>
                <div className="stat-card-label">{stat.label}</div>
              </div>
            </Col>
          ))}
        </Row>

        {!loading &&
          stats.reservationTrend.length === 0 &&
          stats.hotDishes.length === 0 && (
            <Empty
              description="暂无统计数据，厨房还在安静地等待第一批预约"
              style={{ marginBottom: 24 }}
            />
          )}

        <Row gutter={[16, 16]}>
          <Col xs={24} lg={12}>
            <Card title="近 7 日预约趋势" bordered={false} className="ck-chart-card">
              <ReactECharts option={reservationTrendOption} style={{ height: 300 }} />
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card title="热门菜品" bordered={false} className="ck-chart-card">
              <ReactECharts option={hotDishesOption} style={{ height: 300 }} />
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card title="评分分布" bordered={false} className="ck-chart-card">
              <ReactECharts option={ratingDistOption} style={{ height: 300 }} />
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card title="用户活跃度" bordered={false} className="ck-chart-card">
              <ReactECharts option={userActivityOption} style={{ height: 300 }} />
            </Card>
          </Col>
        </Row>
      </Spin>
    </div>
  );
}
