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
  username?: string
  phone?: string
  wechatId?: string
  nickname?: string
  avatarUrl?: string
}

/**
 * 微信 code 登录（仅需 wx.login 返回的 code；资料字段可选）。
 */
export async function wxLogin(
  code: string,
  profile?: WxLoginProfile
): Promise<{ login: LoginResult; user: User }> {
  const body: Record<string, string> = { code }
  if (profile?.username?.trim()) body.username = profile.username.trim()
  if (profile?.phone?.trim()) body.phone = profile.phone.trim()
  if (profile?.wechatId?.trim()) body.wechatId = profile.wechatId.trim()
  if (profile?.nickname?.trim()) body.nickname = profile.nickname.trim()
  if (profile?.avatarUrl?.trim()) body.avatarUrl = profile.avatarUrl.trim()

  const raw = await post<LoginVoRaw>('/api/auth/wx-login', body, { showLoading: true })
  const login: LoginResult = {
    token: raw.token,
    expireSeconds: raw.expireSeconds,
    userId: asId(raw.userId),
    displayName: raw.displayName || raw.username || '微信用户',
    principalType: raw.principalType
  }
  const user: User = {
    id: login.userId,
    username: raw.username || profile?.username,
    nickname: login.displayName,
    phone: profile?.phone,
    wechatId: profile?.wechatId,
    avatarUrl: profile?.avatarUrl
  }
  return { login, user }
}
