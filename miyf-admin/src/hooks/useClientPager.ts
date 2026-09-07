import { useEffect, useMemo, useState } from 'react';
import { slicePage } from '@/api/page';

/**
 * 后端无分页接口时的客户端分页。
 * 返回当前页 records + Ant Design Table pagination。
 */
export function useClientPager<T>(all: T[], initialPageSize = 10) {
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(initialPageSize);

  useEffect(() => {
    const maxPage = Math.max(1, Math.ceil((all.length || 0) / pageSize) || 1);
    if (page > maxPage) {
      setPage(maxPage);
    }
  }, [all.length, page, pageSize]);

  const result = useMemo(() => slicePage(all, page, pageSize), [all, page, pageSize]);

  return {
    page,
    pageSize,
    setPage,
    setPageSize,
    data: result.records,
    total: result.total,
    pagination: {
      current: page,
      pageSize,
      total: result.total,
      showSizeChanger: true as const,
      onChange: (p: number, ps: number) => {
        setPage(p);
        setPageSize(ps);
      },
    },
  };
}
