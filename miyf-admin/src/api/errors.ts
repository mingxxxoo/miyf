import { message } from 'antd';
import { ApiError } from '@/api/http';

/** 统一把未知错误转成可读文案。 */
export function getErrorMessage(err: unknown, fallback = '操作失败'): string {
  if (err instanceof ApiError) {
    return err.message || fallback;
  }
  if (err instanceof Error && err.message) {
    return err.message;
  }
  return fallback;
}

/** 统一 toast 业务错误；401 已由 http 拦截器处理跳转。 */
export function notifyError(err: unknown, fallback = '操作失败'): void {
  if (err instanceof ApiError && err.code === 40100) {
    return;
  }
  message.error(getErrorMessage(err, fallback));
}

/** HTTP 状态友好提示（用于非 ApiResult 响应）。 */
export function httpStatusMessage(status?: number): string {
  if (status === 403) return '没有权限执行此操作';
  if (status === 404) return '资源不存在';
  if (status === 500) return '服务暂时不可用，请稍后重试';
  if (status && status >= 500) return '服务异常，请稍后重试';
  return '网络异常，请检查连接后重试';
}
