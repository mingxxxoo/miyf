import { get } from '@/api/http';
import { asPage, toPageParams, sid, strOrUndef } from '@/api/page';
import type { OperationLog, PageQuery, PageResult } from '@/types';

function mapLog(raw: Record<string, unknown>): OperationLog {
  return {
    id: sid(raw.id),
    operatorId: raw.operatorId != null ? sid(raw.operatorId) : undefined,
    operatorName: strOrUndef(raw.operatorName),
    action: String(raw.action ?? ''),
    module: strOrUndef(raw.module),
    detail: strOrUndef(raw.detail ?? raw.operationDetail),
    ip: strOrUndef(raw.ip ?? raw.requestIp),
    createTime: String(raw.createTime ?? ''),
    operationType: strOrUndef(raw.operationType),
    requestPath: strOrUndef(raw.requestPath ?? raw.requestUri),
    requestUri: strOrUndef(raw.requestUri),
    requestMethod: strOrUndef(raw.requestMethod),
    result: strOrUndef(raw.result ?? raw.status),
    before: strOrUndef(raw.before ?? raw.beforeData),
    after: strOrUndef(raw.after ?? raw.afterData),
  };
}

export type OperationLogQuery = PageQuery & {
  operationType?: string;
  module?: string;
};

/**
 * 操作日志 API（URL/字段协议不变；前端安全映射可选字段）。
 */
export const operationLogApi = {
  page: async (params?: OperationLogQuery): Promise<PageResult<OperationLog>> => {
    const raw = await get<PageResult<Record<string, unknown>> | Record<string, unknown>[]>(
      '/admin/operation-logs',
      toPageParams(params),
    );
    const page = asPage(raw, params?.page, params?.pageSize);
    return {
      ...page,
      records: (page.records ?? []).map((r) => mapLog(r as Record<string, unknown>)),
    };
  },
};
