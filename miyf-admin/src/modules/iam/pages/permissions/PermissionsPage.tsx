import { useCallback, useEffect, useMemo, useState } from 'react';
import PageTable from '@/components/PageTable';
import { iamPermissionApi, type IamPermission } from '@/modules/iam/api';

export default function PermissionsPage() {
  const [loading, setLoading] = useState(false);
  const [all, setAll] = useState<IamPermission[]>([]);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      setAll(await iamPermissionApi.list());
    } catch {
      setAll([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void fetchData();
  }, [fetchData]);

  const data = useMemo(() => {
    const start = (page - 1) * pageSize;
    return all.slice(start, start + pageSize);
  }, [all, page, pageSize]);

  return (
    <div className="ck-page">
      <h2 className="ck-page-title">权限管理</h2>
      <PageTable<IamPermission>
        title="权限列表（启动扫描）"
        loading={loading}
        rowKey="id"
        columns={[
          { title: '编码', dataIndex: 'code' },
          { title: '名称', dataIndex: 'name' },
          { title: '权限组', dataIndex: 'groupCode', render: (v?: string) => v || '—' },
          { title: '说明', dataIndex: 'description', render: (v?: string) => v || '—' },
        ]}
        dataSource={data}
        pagination={{
          current: page,
          pageSize,
          total: all.length,
          onChange: (p, ps) => {
            setPage(p);
            setPageSize(ps);
          },
        }}
        locale={{ emptyText: '暂无权限定义' }}
      />
    </div>
  );
}
