import { useCallback, useEffect, useState } from 'react';
import { Button, Descriptions, Input, Select, Space, Typography } from 'antd';
import SearchForm from '@/components/SearchForm';
import PageTable from '@/components/PageTable';
import { operationLogApi } from '@/modules/system/operationLog';
import type { OperationLog } from '@/types';
import {
  DetailDrawer,
  EmptyState,
  FilterPanel,
  PageHeader,
  PageToolbar,
  StatusBadge,
} from '@/ui';
import { notifyError } from '@/api/errors';

const RESULT_MAP = {
  SUCCESS: { color: 'success', text: '成功' },
  FAIL: { color: 'error', text: '失败' },
  FAILED: { color: 'error', text: '失败' },
  ERROR: { color: 'error', text: '失败' },
};

/**
 * 操作日志：筛选 + 详情抽屉（系统中心与业务路由共用）。
 */
export default function OperationLogsPage() {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<OperationLog[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [keyword, setKeyword] = useState('');
  const [operationType, setOperationType] = useState<string | undefined>();
  const [detail, setDetail] = useState<OperationLog | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await operationLogApi.page({
        page,
        pageSize,
        keyword: keyword || undefined,
        operationType: operationType || undefined,
      });
      setData(res.records ?? []);
      setTotal(res.total ?? 0);
    } catch (err) {
      setData([]);
      setTotal(0);
      notifyError(err, '加载操作日志失败');
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, keyword, operationType]);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  return (
    <div className="ck-page">
      <PageHeader title="操作日志" description="按操作人、模块、类型与时间追溯变更" />
      <FilterPanel>
        <SearchForm
          fields={[
            {
              name: 'keyword',
              label: '关键词',
              element: <Input placeholder="操作人 / 动作 / 模块" allowClear />,
            },
            {
              name: 'operationType',
              label: '类型',
              element: (
                <Select
                  allowClear
                  placeholder="操作类型"
                  options={[
                    { value: 'CREATE', label: '创建' },
                    { value: 'UPDATE', label: '更新' },
                    { value: 'DELETE', label: '删除' },
                    { value: 'LOGIN', label: '登录' },
                    { value: 'OTHER', label: '其他' },
                  ]}
                />
              ),
            },
          ]}
          loading={loading}
          onSearch={(v) => {
            setKeyword(String(v.keyword ?? ''));
            setOperationType(v.operationType ? String(v.operationType) : undefined);
            setPage(1);
          }}
        />
      </FilterPanel>

      <PageToolbar
        left={<Typography.Text type="secondary">共 {total} 条</Typography.Text>}
        right={
          <Button onClick={() => void fetchData()} loading={loading}>
            刷新
          </Button>
        }
      />

      <PageTable<OperationLog>
        title="日志列表"
        loading={loading}
        columns={[
          { title: '操作人', dataIndex: 'operatorName', render: (v?: string) => v || '—' },
          { title: '模块', dataIndex: 'module', render: (v?: string) => v || '—' },
          { title: '动作', dataIndex: 'action' },
          {
            title: '详情',
            dataIndex: 'detail',
            ellipsis: true,
            render: (v?: string) => v || '—',
          },
          { title: 'IP', dataIndex: 'ip', width: 130, render: (v?: string) => v || '—' },
          { title: '时间', dataIndex: 'createTime', width: 180 },
          {
            title: '操作',
            width: 90,
            render: (_, row) => (
              <Button type="link" size="small" onClick={() => setDetail(row)}>
                详情
              </Button>
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
        locale={{ emptyText: <EmptyState description="暂无操作记录" /> }}
      />

      <DetailDrawer
        open={!!detail}
        onClose={() => setDetail(null)}
        title="操作详情"
        width={520}
      >
        {detail ? (
          <Descriptions column={1} size="small" bordered>
            <Descriptions.Item label="操作人">
              {detail.operatorName || detail.operatorId || '—'}
            </Descriptions.Item>
            <Descriptions.Item label="模块">{detail.module || '—'}</Descriptions.Item>
            <Descriptions.Item label="动作">{detail.action || '—'}</Descriptions.Item>
            <Descriptions.Item label="类型">
              {detail.operationType || '—'}
            </Descriptions.Item>
            <Descriptions.Item label="请求路径">
              {detail.requestPath || detail.requestUri || '—'}
            </Descriptions.Item>
            <Descriptions.Item label="IP">{detail.ip || '—'}</Descriptions.Item>
            <Descriptions.Item label="执行结果">
              {detail.result ? (
                <StatusBadge code={detail.result} map={RESULT_MAP} />
              ) : (
                '—'
              )}
            </Descriptions.Item>
            <Descriptions.Item label="详情">
              <Space direction="vertical" style={{ width: '100%' }}>
                <Typography.Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
                  {detail.detail || '—'}
                </Typography.Paragraph>
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="时间">{detail.createTime || '—'}</Descriptions.Item>
          </Descriptions>
        ) : null}
      </DetailDrawer>
    </div>
  );
}
