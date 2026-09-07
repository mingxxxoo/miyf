import axios, {
  type AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios';
import type { ApiResult } from '@/types';

const TOKEN_KEY = 'ck_admin_token';

export const getToken = (): string | null => localStorage.getItem(TOKEN_KEY);

export const setToken = (token: string): void => {
  localStorage.setItem(TOKEN_KEY, token);
};

export const clearToken = (): void => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem('ck-auth-storage');
};

/** 带业务码与附加数据的 API 错误（登录风控等） */
export class ApiError extends Error {
  code: number;
  data?: unknown;

  constructor(message: string, code = -1, data?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.data = data;
  }
}

const http = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

http.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError) => Promise.reject(error),
);

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown> | undefined;
    if (body && typeof body === 'object' && body.code === 40100) {
      clearToken();
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
      return Promise.reject(new ApiError(body.message || '未登录或登录已过期', 40100, body.data));
    }
    return response;
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status;
    const body = error.response?.data;
    const code = body?.code;
    if (status === 401 || code === 40100) {
      clearToken();
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    if (body && typeof body === 'object' && 'code' in body && body.code !== 0) {
      return Promise.reject(new ApiError(body.message || '请求失败', body.code, body.data));
    }
    if (status === 403) {
      return Promise.reject(new ApiError('没有权限执行此操作', 403));
    }
    if (status && status >= 500) {
      return Promise.reject(new ApiError('服务暂时不可用，请稍后重试', status));
    }
    if (!error.response) {
      return Promise.reject(new ApiError('网络异常，请检查连接后重试', -1));
    }
    return Promise.reject(error);
  },
);

/** Unwrap ApiResult; throw when business code !== 0 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiResult<T>>(config);
  const body = response.data;

  if (body && typeof body === 'object' && 'code' in body) {
    if (body.code !== 0) {
      if (body.code === 40100) {
        clearToken();
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
      }
      throw new ApiError(body.message || '请求失败', body.code, body.data);
    }
    return body.data;
  }

  return body as unknown as T;
}

export function get<T>(url: string, params?: Record<string, unknown>) {
  return request<T>({ method: 'GET', url, params });
}

export function post<T>(url: string, data?: unknown) {
  return request<T>({ method: 'POST', url, data });
}

export function put<T>(url: string, data?: unknown) {
  return request<T>({ method: 'PUT', url, data });
}

export function del<T>(url: string, params?: Record<string, unknown>) {
  return request<T>({ method: 'DELETE', url, params });
}

export default http;
