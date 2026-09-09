import { getToken, ApiError, clearToken } from '@/api/http';

export type UploadResult = {
  url: string;
  id?: string;
  path?: string;
  appCode?: string;
  [key: string]: unknown;
};

export type UploadOptions = {
  /** 产品应用编码，如 kitchen / health */
  appCode: string;
  source?: string;
  temp?: boolean;
  compress?: boolean;
  accessPermission?: 'PUBLIC' | 'AUTHENTICATED' | 'OWNER' | 'ADMIN' | 'DENY';
  path?: string;
};

/**
 * 统一上传封装（FormData → /upload）。
 */
export async function uploadFile(
  file: File,
  options: UploadOptions,
): Promise<UploadResult> {
  const path = options.path ?? '/upload';
  const form = new FormData();
  form.append('file', file);
  form.append('appCode', options.appCode);
  if (options.source) {
    form.append('source', options.source);
  }
  if (options.temp != null) {
    form.append('temp', String(options.temp));
  }
  if (options.compress != null) {
    form.append('compress', String(options.compress));
  }
  if (options.accessPermission) {
    form.append('accessPermission', options.accessPermission);
  }
  const token = getToken();
  const res = await fetch(`/api${path}`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    body: form,
  });
  const body = (await res.json().catch(() => null)) as
    | { code?: number; message?: string; data?: UploadResult }
    | null;

  if (res.status === 401 || body?.code === 40100) {
    clearToken();
    if (window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
    throw new ApiError(body?.message || '未登录或登录已过期', 40100);
  }
  if (!res.ok) {
    throw new ApiError(body?.message || `上传失败(${res.status})`, body?.code ?? res.status);
  }
  if (body && typeof body.code === 'number' && body.code !== 0) {
    throw new ApiError(body.message || '上传失败', body.code, body.data);
  }
  const data = (body?.data ?? body) as UploadResult | undefined;
  if (!data?.url) {
    throw new ApiError('上传成功但未返回文件地址');
  }
  return data;
}
