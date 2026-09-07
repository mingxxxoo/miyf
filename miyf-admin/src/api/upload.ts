import { getToken, ApiError, clearToken } from '@/api/http';

export type UploadResult = {
  url: string;
  [key: string]: unknown;
};

/**
 * 统一上传封装（FormData → /upload）。
 * 不改变后端路径与字段。
 */
export async function uploadFile(
  file: File,
  path = '/upload',
): Promise<UploadResult> {
  const form = new FormData();
  form.append('file', file);
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
