import { post } from '@/api/request'
import type { LoginResult, User } from '@/types'
import { asId } from '@/utils/id'

interface LoginVoRaw {
  token: string
  expireSeconds?: number
  userId: string
  displayName: string
  principalType?: string
}

/**
 * 微信 code 登录（dev 环境后端 Mock）。
 */
export async function wxLogin(code: string): Promise<{ login: LoginResult; user: User }> {
  const raw = await post<LoginVoRaw>('/api/auth/wx-login', { code }, { showLoading: true })
  const login: LoginResult = {
    token: raw.token,
    expireSeconds: raw.expireSeconds,
    userId: asId(raw.userId),
    displayName: raw.displayName || '厨房朋友',
    principalType: raw.principalType
  }
  const user: User = {
    id: login.userId,
    nickname: login.displayName
  }
  return { login, user }
}
