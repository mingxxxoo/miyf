import { useCallback, useEffect, useState } from 'react';
import { Button, Input, Space, message } from 'antd';
import PageTable from '@/components/PageTable';
import { dishAuditApi, notifyError } from '@/api';
import { EmptyState, PageHeader, StatusBadge } from '@/ui';
import { confirmAction } from '@/ui';
import { DISH_AUDIT_STATUS } from '@/constants/status';

type AuditTask = {
  taskId: string;
  dishId: string;
  kitchenName: string;
  dishName: string;
};

export default function DishAuditsPage() {
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<AuditTask[]>([]);
  const [actingId, setActingId] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      setData(await dishAuditApi.list());
    } catch (err) {
      setData([]);
      notifyError(err, '加载审核待办失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const approve = (row: AuditTask) => {
    confirmAction({
      title: '通过审核',
      content: `确定通过「${row.dishName}」吗？`,
      onOk: async () => {
        setActingId(row.taskId);
        try {
          await dishAuditApi.approve(row.taskId);
          message.success('已通过');
          void fetchData();
        } catch (err) {
          notifyError(err, '操作失败');
        } finally {
          setActingId(null);
        }
      },
    });
  };

  const reject = (row: AuditTask) => {
    let reason = '';
    confirmAction({
      title: '驳回菜品',
      content: (
        <Input.TextArea
          rows={3}
          placeholder="请填写驳回原因"
          onChange={(e) => {
            reason = e.target.value;
          }}
        />
      ),
      onOk: async () => {
        if (!reason.trim()) {
          message.warning('驳回原因不能为空');
          throw new Error('empty reason');
        }
        setActingId(row.taskId);
        try {
          await dishAuditApi.reject(row.taskId, reason.trim());
          message.success('已驳回');
          void fetchData();
        } catch (err) {
          notifyError(err, '操作失败');
        } finally {
          setActingId(null);
        }
      },
    });
  };

  return (
    <div className="ck-page">
      <PageHeader title="菜品审核" description="审核厨师提交的菜品，通过后厨师才可上架。系统无价格与支付。" />
      <PageTable<AuditTask>
        title="待办"
        loading={loading}
        rowKey="taskId"
        columns={[
          { title: '菜品', dataIndex: 'dishName' },
          { title: '厨房', dataIndex: 'kitchenName' },
          {
            title: '状态',
            render: () => <StatusBadge code="PENDING_REVIEW" map={DISH_AUDIT_STATUS} />,
          },
          {
            title: '操作',
            render: (_, row) => (
              <Space>
                <Button type="link" loading={actingId === row.taskId} onClick={() => approve(row)}>
                  通过
                </Button>
                <Button type="link" danger loading={actingId === row.taskId} onClick={() => reject(row)}>
                  驳回
                </Button>
              </Space>
            ),
          },
        ]}
        dataSource={data}
        locale={{ emptyText: <EmptyState description="暂无待审核菜品" /> }}
      />
    </div>
  );
}
