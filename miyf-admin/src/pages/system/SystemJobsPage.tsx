import { useCallback, useEffect, useState } from 'react';
import { Button, Popconfirm, Space, Table, Tag, message } from 'antd';
import { sysJobApi, type SysJob } from '@/pages/system/api';

/**
 * 定时任务控制台：列表、启停、手动触发。
 */
export default function SystemJobsPage() {
  const [loading, setLoading] = useState(false);
  const [jobs, setJobs] = useState<SysJob[]>([]);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      setJobs(await sysJobApi.list());
    } catch (err) {
      setJobs([]);
      message.error(err instanceof Error ? err.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const run = async (code: string, action: 'trigger' | 'enable' | 'disable') => {
    try {
      if (action === 'trigger') await sysJobApi.trigger(code);
      if (action === 'enable') await sysJobApi.enable(code);
      if (action === 'disable') await sysJobApi.disable(code);
      message.success('操作成功');
      await fetchData();
    } catch (err) {
      message.error(err instanceof Error ? err.message : '操作失败');
    }
  };

  return (
    <div className="ck-page">
      <h2 className="ck-page-title">定时任务</h2>
      <Space style={{ marginBottom: 16 }}>
        <Button onClick={() => void fetchData()}>刷新</Button>
      </Space>
      <Table
        rowKey="code"
        loading={loading}
        dataSource={jobs}
        pagination={false}
        columns={[
          { title: '编码', dataIndex: 'code', width: 180 },
          { title: '名称', dataIndex: 'name', width: 140 },
          { title: 'Cron', dataIndex: 'cron', width: 160, ellipsis: true },
          {
            title: '调度',
            dataIndex: 'enabled',
            width: 90,
            render: (v: boolean) =>
              v ? <Tag color="green">启用</Tag> : <Tag>停用</Tag>,
          },
          {
            title: '最近状态',
            dataIndex: 'lastStatus',
            width: 100,
            render: (v: string) => <Tag>{v || '-'}</Tag>,
          },
          { title: '最近开始', dataIndex: 'lastStartedAt', width: 180 },
          { title: '最近结束', dataIndex: 'lastFinishedAt', width: 180 },
          { title: '错误', dataIndex: 'lastError', ellipsis: true },
          {
            title: '操作',
            width: 220,
            fixed: 'right',
            render: (_, row) => (
              <Space>
                <Popconfirm title="立即执行一次？" onConfirm={() => void run(row.code, 'trigger')}>
                  <Button type="link" size="small">
                    触发
                  </Button>
                </Popconfirm>
                {row.enabled ? (
                  <Button type="link" size="small" onClick={() => void run(row.code, 'disable')}>
                    停用
                  </Button>
                ) : (
                  <Button type="link" size="small" onClick={() => void run(row.code, 'enable')}>
                    启用
                  </Button>
                )}
              </Space>
            ),
          },
        ]}
        scroll={{ x: 1200 }}
      />
    </div>
  );
}
