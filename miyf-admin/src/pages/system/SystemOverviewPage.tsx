import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Col, List, Row, Space, Typography, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import {
  EmptyState,
  LoadingState,
  MetricCard,
  PageHeader,
  SettingSection,
  StatusBadge,
} from '@/ui';
import { notifyError } from '@/api/errors';
import { operationLogApi } from '@/modules/system/operationLog';
import type { OperationLog } from '@/types';
import { iamRoleApi } from '@/modules/iam/api';
import {
  sysJobApi,
  sysMonitorApi,
  sysNotificationApi,
  type MonitorOverview,
  type SysJob,
} from '@/modules/system/api';
import { healthApi } from '@/modules/health/api';
import { useAuthStore } from '@/stores/authStore';
import { usePermissionStore } from '@/stores/permissionStore';
import { ENABLED_STATUS } from '@/constants/status';

function serviceHealth(overview: MonitorOverview | null): 'ENABLED' | 'DISABLED' | undefined {
  const comps = overview?.components ?? [];
  if (!comps.length) return undefined;
  const relevant = comps.filter((c) => {
    const n = c.name.toLowerCase();
    return n.includes('db') || n.includes('redis') || n.includes('database');
  });
  const target = relevant.length ? relevant : comps;
  return target.every((c) => String(c.status).toUpperCase() === 'UP') ? 'ENABLED' : 'DISABLED';
}

function componentStatus(overview: MonitorOverview | null, name: string) {
  const hit = overview?.components?.find((c) => c.name.toLowerCase().includes(name.toLowerCase()));
  return hit?.status || '—';
}

/**
 * 系统管理中心首页：服务状态、待办、快捷入口、最近操作。
 */
export default function SystemOverviewPage() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const hasPermission = usePermissionStore((s) => s.hasPermission);
  const hasAnyPermission = usePermissionStore((s) => s.hasAnyPermission);
  const [loading, setLoading] = useState(true);
  const [monitor, setMonitor] = useState<MonitorOverview | null>(null);
  const [jobs, setJobs] = useState<SysJob[]>([]);
  const [unread, setUnread] = useState(0);
  const [authSummary, setAuthSummary] = useState({ roleCount: 0, userCount: 0 });
  const [syncFail, setSyncFail] = useState(0);
  const [recentLogs, setRecentLogs] = useState<OperationLog[]>([]);

  const fetchAll = useCallback(async () => {
    setLoading(true);
    const userKey = user?.username || user?.id || '';
    try {
      const tasks: Promise<void>[] = [];

      if (hasPermission('sys:monitor:view')) {
        tasks.push(
          sysMonitorApi
            .overview()
            .then(setMonitor)
            .catch(() => {
              setMonitor(null);
              message.warning('监控概览加载失败');
            }),
        );
      }
      if (hasPermission('sys:job:list')) {
        tasks.push(
          sysJobApi
            .list()
            .then(setJobs)
            .catch(() => {
              setJobs([]);
              message.warning('定时任务加载失败');
            }),
        );
      }
      if (hasPermission('iam:role:list')) {
        tasks.push(
          iamRoleApi
            .authSummary()
            .then(setAuthSummary)
            .catch(() => {
              setAuthSummary({ roleCount: 0, userCount: 0 });
              message.warning('授权汇总加载失败');
            }),
        );
      }
      if (userKey && hasAnyPermission(['sys:notify:list', 'sys:notify:send'])) {
        tasks.push(
          sysNotificationApi
            .listInbox(userKey, 50)
            .then((list) => setUnread(list.filter((i) => !i.read).length))
            .catch(() => {
              setUnread(0);
              message.warning('站内信加载失败');
            }),
        );
      }
      if (hasAnyPermission(['health:provider:list', 'health:sync:trigger', 'health:overview:view'])) {
        tasks.push(
          healthApi
            .listSyncRuns({ limit: 50 })
            .then((runs) => {
              setSyncFail(
                (runs ?? []).filter((r) => String(r.status || '').toUpperCase() === 'FAILED')
                  .length,
              );
            })
            .catch(() => {
              setSyncFail(0);
              message.warning('健康同步记录加载失败');
            }),
        );
      }
      if (hasPermission('operation-log:list')) {
        tasks.push(
          operationLogApi
            .page({ page: 1, pageSize: 8 })
            .then((page) => setRecentLogs(page.records ?? []))
            .catch(() => {
              setRecentLogs([]);
              message.warning('操作日志加载失败');
            }),
        );
      }

      await Promise.all(tasks);
    } catch (err) {
      notifyError(err, '加载系统概览失败');
    } finally {
      setLoading(false);
    }
  }, [hasAnyPermission, hasPermission, user?.id, user?.username]);

  useEffect(() => {
    void fetchAll();
  }, [fetchAll]);

  const failedJobs = useMemo(
    () => jobs.filter((j) => String(j.lastStatus || '').toUpperCase().includes('FAIL')),
    [jobs],
  );

  const shortcuts = useMemo(() => {
    const all = [
      { path: '/system/role-auth', label: '用户授权', show: hasPermission('iam:role:list') },
      { path: '/system/config', label: '基础设置', show: hasPermission('sys:config:list') },
      { path: '/system/jobs', label: '定时任务', show: hasPermission('sys:job:list') },
      { path: '/system/monitor', label: '系统监控', show: hasPermission('sys:monitor:view') },
      {
        path: '/system/notifications',
        label: '通知中心',
        show: hasAnyPermission(['sys:notify:list', 'sys:notify:send']),
      },
      {
        path: '/system/health-sync',
        label: '健康同步',
        show: hasAnyPermission(['health:provider:list', 'health:sync:trigger']),
      },
    ];
    return all.filter((i) => i.show);
  }, [hasAnyPermission, hasPermission]);

  if (loading && !monitor && !jobs.length) {
    return (
      <div className="ck-page">
        <PageHeader title="系统概览" description="系统管理中心" />
        <LoadingState tip="正在汇总系统状态…" />
      </div>
    );
  }

  return (
    <div className="ck-page">
      <PageHeader
        title="系统概览"
        description="一眼查看服务健康、待办事项与常用入口"
        extra={
          <Button onClick={() => void fetchAll()} loading={loading}>
            刷新
          </Button>
        }
      />

      <SettingSection title="服务状态" description="API / 数据库 / Redis / 文件存储与运行指标">
        <Row gutter={[16, 16]}>
          <Col xs={12} md={6}>
            <MetricCard
              label="服务"
              value={
                serviceHealth(monitor) ? (
                  <StatusBadge code={serviceHealth(monitor)} map={ENABLED_STATUS} />
                ) : (
                  '—'
                )
              }
              hint={monitor?.jvm?.javaVersion || '依赖组件汇总'}
            />
          </Col>
          <Col xs={12} md={6}>
            <MetricCard
              label="数据库"
              value={componentStatus(monitor, 'db')}
            />
          </Col>
          <Col xs={12} md={6}>
            <MetricCard label="Redis" value={componentStatus(monitor, 'redis') || '—'} />
          </Col>
          <Col xs={12} md={6}>
            <MetricCard
              label="文件存储"
              value={
                monitor?.disk
                  ? `${Math.round(((monitor.disk.usable ?? 0) / Math.max(monitor.disk.total || 1, 1)) * 100)}% 可用`
                  : '—'
              }
            />
          </Col>
        </Row>
      </SettingSection>

      <SettingSection title="待处理事项">
        <Row gutter={[16, 16]}>
          <Col xs={12} md={6}>
            <MetricCard
              label="未读通知"
              value={unread}
              onClick={() => navigate('/system/notifications')}
            />
          </Col>
          <Col xs={12} md={6}>
            <MetricCard
              label="失败任务"
              value={failedJobs.length}
              onClick={() => navigate('/system/jobs')}
            />
          </Col>
          <Col xs={12} md={6}>
            <MetricCard
              label="健康同步异常"
              value={syncFail}
              onClick={() => navigate('/system/health-sync')}
            />
          </Col>
          <Col xs={12} md={6}>
            <MetricCard
              label="已授权人员"
              value={authSummary.userCount}
              hint={`角色 ${authSummary.roleCount}`}
              onClick={() => navigate('/system/role-auth')}
            />
          </Col>
        </Row>
      </SettingSection>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={10}>
          <SettingSection title="快捷操作">
            {shortcuts.length ? (
              <Space wrap>
                {shortcuts.map((s) => (
                  <Button key={s.path} onClick={() => navigate(s.path)}>
                    {s.label}
                  </Button>
                ))}
              </Space>
            ) : (
              <EmptyState description="暂无可用快捷入口" />
            )}
          </SettingSection>
        </Col>
        <Col xs={24} lg={14}>
          <SettingSection
            title="最近操作"
            extra={
              hasPermission('operation-log:list') ? (
                <Typography.Link onClick={() => navigate('/system/operation-logs')}>
                  查看全部
                </Typography.Link>
              ) : null
            }
          >
            {recentLogs.length ? (
              <List
                size="small"
                dataSource={recentLogs}
                renderItem={(item) => (
                  <List.Item>
                    <List.Item.Meta
                      title={`${item.operatorName || item.operatorId || '系统'} · ${item.action || item.module || '操作'}`}
                      description={item.createTime || item.detail || '—'}
                    />
                  </List.Item>
                )}
              />
            ) : (
              <EmptyState description="暂无操作记录" />
            )}
          </SettingSection>
        </Col>
      </Row>

      {failedJobs.length > 0 ? (
        <SettingSection title="失败任务明细" danger>
          <List
            size="small"
            dataSource={failedJobs}
            renderItem={(j) => (
              <List.Item
                actions={[
                  <Button
                    key="go"
                    type="link"
                    onClick={() => {
                      message.info(`请在定时任务页处理：${j.code}`);
                      navigate('/system/jobs');
                    }}
                  >
                    处理
                  </Button>,
                ]}
              >
                <List.Item.Meta
                  title={`${j.name} (${j.code})`}
                  description={j.lastError || j.lastStatus || '执行失败'}
                />
              </List.Item>
            )}
          />
        </SettingSection>
      ) : null}
    </div>
  );
}
