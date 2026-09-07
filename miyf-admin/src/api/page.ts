import type { PageQuery, PageResult } from '@/types';

/** UI pageSize → 后端 AbstractCondition.rows */
export function toPageParams(
  params?: PageQuery | Record<string, unknown>,
): Record<string, unknown> | undefined {
  if (!params) return undefined;
  const src = params as Record<string, unknown>;
  const { pageSize, rows, page, ...rest } = src;
  return {
    ...rest,
    page: (page as number | undefined) ?? 1,
    rows: (rows as number | undefined) ?? (pageSize as number | undefined) ?? 10,
  };
}

/** 安全归一化分页结果（兼容数组伪分页）。 */
export function asPage<T>(
  raw: PageResult<T> | T[] | null | undefined,
  fallbackPage = 1,
  fallbackSize = 10,
): PageResult<T> {
  if (!raw) {
    return { records: [], total: 0, page: fallbackPage, pageSize: fallbackSize };
  }
  if (Array.isArray(raw)) {
    return { records: raw, total: raw.length, page: 1, pageSize: raw.length || fallbackSize };
  }
  return {
    records: raw.records ?? [],
    total: Number(raw.total ?? 0),
    page: Number(raw.page ?? fallbackPage),
    pageSize: Number(raw.pageSize ?? fallbackSize),
  };
}

/** 客户端本地分页（仅用于后端无 page 接口的列表）。 */
export function slicePage<T>(all: T[], page: number, pageSize: number): PageResult<T> {
  const start = Math.max(0, (page - 1) * pageSize);
  return {
    records: all.slice(start, start + pageSize),
    total: all.length,
    page,
    pageSize,
  };
}

export function sid(v: unknown): string {
  return v == null ? '' : String(v);
}

export function strOrUndef(v: unknown): string | undefined {
  if (v == null) return undefined;
  const s = String(v);
  return s.length ? s : undefined;
}
