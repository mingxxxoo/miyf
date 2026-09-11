import { get, post, put, getToken, clearToken } from '@/api/request'
import type { LoginResult, User } from '@/types'
import { asId } from '@/utils/id'
import { toAbsoluteResourceUrl } from '@/utils/resourceUrl'
import Taro from '@tarojs/taro'

const BASE_URL = process.env.TARO_APP_API_BASE || 'https://www.miyf.cn'

interface LoginVoRaw {
  token: string
  expireSeconds?: number
  userId: string
  displayName: string
  username?: string
  avatarUrl?: string
  principalType?: string
}

interface UserVoRaw {
  id: string
  username?: string
  nickname?: string
  avatarUrl?: string
  phone?: string
  wechatId?: string
  status?: string
}

export interface WxLoginProfile {
  username?: string
  phone?: string
  wechatId?: string
  nickname?: string
  avatarUrl?: string
}

function mapUser(raw: UserVoRaw | LoginVoRaw, profile?: WxLoginProfile): User {
  const loginLike = raw as LoginVoRaw
  const userLike = raw as UserVoRaw
  const avatar =
    toAbsoluteResourceUrl(userLike.avatarUrl || loginLike.avatarUrl) ||
    profile?.avatarUrl
  return {
    id: asId(String(loginLike.userId ?? userLike.id ?? '')),
    username: userLike.username || loginLike.username || profile?.username,
    nickname:
      userLike.nickname ||
      loginLike.displayName ||
      profile?.nickname ||
      loginLike.username ||
      '微信用户',
    phone: userLike.phone || profile?.phone,
    wechatId: userLike.wechatId || profile?.wechatId,
    avatarUrl: avatar
  }
}

/**
 * 微信 code 登录（资料字段可选；首次登录建议传昵称）。
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
  // 仅本站 /r/ 路径传给登录接口；外站 https / 本地临时路径登录后再 upload
  const avatar = profile?.avatarUrl?.trim()
  if (avatar && (/^\/r\//.test(avatar) || /^\/api\/r\//.test(avatar))) {
    body.avatarUrl = avatar
  }

  const raw = await post<LoginVoRaw>('/api/auth/wx-login', body, { showLoading: true })
  const login: LoginResult = {
    token: raw.token,
    expireSeconds: raw.expireSeconds,
    userId: asId(raw.userId),
    displayName: raw.displayName || raw.username || profile?.nickname || '微信用户',
    principalType: raw.principalType
  }
  const user = mapUser(raw, profile)
  user.id = login.userId
  user.nickname = login.displayName
  return { login, user }
}

/** 拉取当前用户资料 */
export async function fetchMyProfile(): Promise<User> {
  const raw = await get<UserVoRaw>('/api/user/me')
  return mapUser(raw)
}

/** 更新昵称 / 头像 URL */
export async function updateMyProfile(payload: {
  nickname?: string
  avatarUrl?: string
}): Promise<User> {
  const raw = await put<UserVoRaw>('/api/user/me', payload, { showLoading: true })
  return mapUser(raw)
}

/**
 * 上传头像（multipart POST），服务端落库并返回最新资料。
 */
export async function uploadMyAvatar(filePath: string): Promise<User> {
  const token = getToken()
  const res = await Taro.uploadFile({
    url: `${BASE_URL}/api/user/me/avatar`,
    filePath,
    name: 'file',
    header: token ? { Authorization: `Bearer ${token}` } : {},
    formData: {}
  })
  if (res.statusCode === 401) {
    clearToken()
    try {
      Taro.eventCenter.trigger('miyf:auth-expired')
    } catch {
      // ignore
    }
    throw new Error('请先登录')
  }
  let body: { code?: number; message?: string; data?: UserVoRaw } | null = null
  try {
    body = typeof res.data === 'string' ? JSON.parse(res.data) : (res.data as typeof body)
  } catch {
    throw new Error('上传头像失败')
  }
  if (body && body.code === 40100) {
    clearToken()
    try {
      Taro.eventCenter.trigger('miyf:auth-expired')
    } catch {
      // ignore
    }
    throw new Error('请先登录')
  }
  if (!body || body.code !== 0 || !body.data) {
    throw new Error(body?.message || '上传头像失败')
  }
  return mapUser(body.data)
}
