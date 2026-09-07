import { useCallback, useEffect, useState } from 'react';
import { Button, Popconfirm, Space, Table, Tag, message } from 'antd';
import { notifyError } from '@/api/errors';
import { ENABLED_STATUS } from '@/constants/status';
import { useSubmitting } from '@/hooks/useSubmitting';
import { sysJobApi, type SysJob } from '@/modules/system/api';
import { EmptyState, PageHeader, StatusBadge } from '@/ui';

/**
 * 定时任务：列表、启停、手动触发（按 code 显示执行中）。
 */
export default function SystemJobsPage() {
  const [loading, setLoading] = useState(false);
  const [jobs, setJobs] = useState<SysJob[]>([]);
  const { submitting, run } = useSubmitting();
  const [activeCode, setActiveCode] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      setJobs(await sysJobApi.list());
    } catch (err) {
      setJobs([]);
      notifyError(err, '加载失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const runAction = (code: string, action: 'trigger' | 'enable' | 'disable') =>
    run(async () => {
      setActiveCode(code);
      try {
        if (action === 'trigger') await sysJobApi.trigger(code);
        if (action === 'enable') await sysJobApi.enable(code);
        if (action === 'disable') await sysJobApi.disable(code);
        message.success(
          action === 'trigger' ? '已触发执行' : action === 'enable' ? '已启用' : '已停用',
        );
        await fetchData();
      } catch (err) {
        notifyError(err, '操作失败');
      } finally {
        setActiveCode(null);
      }
    });

  return (
    <div className="ck-page">
      <PageHeader
        title="定时任务"
        description="查看调度状态并手动启停 / 触发"
        extra={
          <Button onClick={() => void fetchData()} loading={loading}>
            刷新
          </Button>
        }
      />
      <Table<SysJob>
        rowKey="code"
        loading={loading}
        dataSource={jobs}
        pagination={false}
        locale={{ emptyText: <EmptyState description="暂无定时任务" /> }}
        scroll={{ x: 1100 }}
        columns={[
          { title: '名称', dataIndex: 'name', width: 160 },
          { title: 'Cron', dataIndex: 'cron', width: 160, ellipsis: true, render: (v?: string) => v || '—' },
          {
            title: '上次开始',
            dataIndex: 'lastStartedTime',
            width: 180,
            render: (v?: string) => v || '—',
          },
          {
            title: '上次结束',
            dataIndex: 'lastFinishedTime',
            width: 180,
            render: (v?: string) => v || '—',
          },
          {
            title: '上次状态',
            dataIndex: 'lastStatus',
            width: 110,
            render: (v?: string) => <Tag>{v || '—'}</Tag>,
          },
          {
            title: '启用',
            dataIndex: 'enabled',
            width: 90,
            render: (v: boolean) => (
              <StatusBadge code={v ? 'ENABLED' : 'DISABLED'} map={ENABLED_STATUS} />
            ),
          },
          {
            title: '下次执行',
            dataIndex: 'nextRunTime',
            width: 180,
            render: (v?: string) => v || '—',
          },
          {
            title: '操作',
            width: 220,
            fixed: 'right',
            render: (_, row) => {
              const busy = submitting && activeCode === row.code;
              return (
                <Space>
                  <Popconfirm
                    title="立即执行一次？"
                    onConfirm={() => void runAction(row.code, 'trigger')}
                  >
                    <Button type="link" size="small" loading={busy}>
                      {busy ? '执行中' : '触发'}
                    </Button>
                  </Popconfirm>
                  {row.enabled ? (
                    <Popconfirm
                      title="确认停用该任务？"
                      onConfirm={() => void runAction(row.code, 'disable')}
                    >
                      <Button type="link" size="small" disabled={busy}>
                        停用
                      </Button>
                    </Popconfirm>
                  ) : (
                    <Popconfirm
                      title="确认启用该任务？"
                      onConfirm={() => void runAction(row.code, 'enable')}
                    >
                      <Button type="link" size="small" disabled={busy}>
                        启用
                      </Button>
                    </Popconfirm>
                  )}
                </Space>
              );
            },
          },
        ]}
      />
    </div>
  );
}
