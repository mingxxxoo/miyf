import { get, post } from './http';
import { sid } from './page';
import type { AdminUser, LoginVo } from '@/types';

export { toPageParams, asPage, slicePage, sid } from './page';
export { notifyError, getErrorMessage } from './errors';
export { uploadFile } from './upload';
export { toResourceUrl, toResourceUrlOrEmpty } from './resourceUrl';
export { operationLogApi } from '@/modules/system/operationLog';
export type { OperationLogQuery } from '@/modules/system/operationLog';

export {
  dashboardApi,
  userApi,
  categoryApi,
  dishApi,
  recipeApi,
  orderApi,
  commentApi,
} from '@/modules/kitchen/api';

export interface LoginResult {
  token: string;
  user: AdminUser;
  permissions: string[];
}

export interface LoginRisk {
  failCount: number;
  captchaRequired: boolean;
  locked: boolean;
  lockedUntil?: number | null;
  lockRemainSeconds: number;
}

export interface CaptchaPayload {
  captchaId: string;
  imageBase64: string;
  expireSeconds: number;
}

export const authApi = {
  login: async (
    username: string,
    password: string,
    captcha?: { captchaId?: string; captchaCode?: string },
  ): Promise<LoginResult> => {
    const vo = await post<LoginVo>('/admin/auth/login', {
      username,
      password,
      captchaId: captcha?.captchaId,
      captchaCode: captcha?.captchaCode,
    });
    const permissions = vo.permissions?.length ? vo.permissions : [];
    const roles = vo.roles ?? [];
    return {
      token: vo.token,
      permissions,
      user: {
        id: sid(vo.userId),
        username: vo.username || username,
        nickname: vo.displayName || vo.username || username,
        roles,
      },
    };
  },
  loginStatus: (username: string) =>
    get<LoginRisk>('/admin/auth/login-status', { username }),
  captcha: () => get<CaptchaPayload>('/admin/auth/captcha'),
  me: async (): Promise<LoginResult> => {
    const vo = await get<LoginVo>('/admin/auth/me');
    const permissions = vo.permissions?.length ? vo.permissions : [];
    return {
      token: vo.token || '',
      permissions,
      user: {
        id: sid(vo.userId),
        username: vo.username || vo.displayName || 'admin',
        nickname: vo.displayName || vo.username || 'admin',
        roles: vo.roles ?? [],
      },
    };
  },
};
