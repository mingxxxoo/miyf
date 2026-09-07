import { post } from '@/api/request'
import type { LoginResult, User } from '@/types'
import { asId } from '@/utils/id'

interface LoginVoRaw {
  token: string
  expireSeconds?: number
  userId: string
  displayName: string
  username?: string
  principalType?: string
}

export interface WxLoginProfile {
  username: string
  phone: string
  wechatId: string
  nickname?: string
  avatarUrl?: string
}

/**
 * 微信 code 登录（须同时提交用户名、手机号、微信号）。
 */
export async function wxLogin(
  code: string,
  profile: WxLoginProfile
): Promise<{ login: LoginResult; user: User }> {
  const raw = await post<LoginVoRaw>(
    '/api/auth/wx-login',
    {
      code,
      username: profile.username,
      phone: profile.phone,
      wechatId: profile.wechatId,
      nickname: profile.nickname || profile.username,
      avatarUrl: profile.avatarUrl
    },
    { showLoading: true }
  )
  const login: LoginResult = {
    token: raw.token,
    expireSeconds: raw.expireSeconds,
    userId: asId(raw.userId),
    displayName: raw.displayName || profile.username,
    principalType: raw.principalType
  }
  const user: User = {
    id: login.userId,
    username: profile.username,
    nickname: login.displayName,
    phone: profile.phone,
    wechatId: profile.wechatId,
    avatarUrl: profile.avatarUrl
  }
  return { login, user }
}
