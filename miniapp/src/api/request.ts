import Taro from '@tarojs/taro'
import type { ApiResult } from '@/types'

const BASE_URL = process.env.TARO_APP_API_BASE || 'http://www.miyf.cn'

const TOKEN_KEY = 'miyf_kitchen_token'

export function getToken(): string {
  return Taro.getStorageSync(TOKEN_KEY) || ''
}

export function setToken(token: string): void {
  Taro.setStorageSync(TOKEN_KEY, token)
}

export function clearToken(): void {
  Taro.removeStorageSync(TOKEN_KEY)
}

interface RequestOptions {
  url: string
  data?: Record<string, unknown> | unknown
  header?: Record<string, string>
  showLoading?: boolean
  showError?: boolean
}

/** 去掉 null/undefined，避免微信把 undefined 序列化成字符串 "undefined" */
function sanitizeRequestData(data: RequestOptions['data']): Record<string, unknown> | undefined {
  if (data == null || typeof data !== 'object' || Array.isArray(data)) {
    return data as Record<string, unknown> | undefined
  }
  const out: Record<string, unknown> = {}
  for (const [key, value] of Object.entries(data as Record<string, unknown>)) {
    if (value === undefined || value === null) continue
    if (value === 'undefined' || value === 'null') continue
    if (value === '') continue
    out[key] = value
  }
  return out
}

/** 从微信 / 业务错误中抽出可读文案（避免 [object Object]） */
function toErrorText(err: unknown): string {
  if (err == null) return ''
  if (typeof err === 'string') return err
  if (typeof err === 'number' || typeof err === 'boolean') return String(err)
  if (err instanceof Error) {
    const msg = err.message
    if (msg && msg !== '[object Object]') return msg
  }
  if (typeof err === 'object') {
    const o = err as Record<string, unknown>
    // 微信 Taro.request fail: { errMsg: 'request:fail ...' }
    if (typeof o.errMsg === 'string' && o.errMsg) return o.errMsg
    if (typeof o.message === 'string' && o.message) return o.message
    if (o.message != null && typeof o.message !== 'object') return String(o.message)
    if (typeof o.error === 'string' && o.error) return o.error
  }
  return ''
}

/** 网络 / 域名类失败转成中文提示 */
function friendlyRequestError(raw: string): string {
  const text = (raw || '').trim()
  if (!text || text === '[object Object]') {
    return '网络请求失败'
  }
  if (/url not in domain|不在以下.?request|合法域名|invalid url/i.test(text)) {
    return '未配置合法请求域名'
  }
  if (/ssl|certificate|https/i.test(text) && /fail/i.test(text)) {
    return 'HTTPS 证书或域名异常'
  }
  if (/timeout/i.test(text)) {
    return '请求超时，请稍后重试'
  }
  if (/request:fail|ERR_CONNECTION|net::|fail/i.test(text)) {
    return '网络不可用，请检查域名'
  }
  return text
}

/** hideLoading 后立刻 showToast 会被微信吞掉，需短暂延迟 */
function showToastAfterLoading(title: string) {
  const safe = friendlyRequestError(toErrorText(title) || title).slice(0, 40)
  setTimeout(() => {
    Taro.showToast({ title: safe || '请求失败', icon: 'none', duration: 3000 })
  }, 100)
}

const USER_KEY = 'miyf_user'
const LEGACY_USER_KEY = 'ck_user'

function clearAuthState() {
  clearToken()
  try {
    Taro.removeStorageSync(USER_KEY)
    Taro.removeStorageSync(LEGACY_USER_KEY)
  } catch {
    // ignore
  }
  try {
    Taro.eventCenter.trigger('miyf:auth-expired')
  } catch {
    // ignore
  }
}

async function request<T>(method: 'GET' | 'POST' | 'PUT' | 'DELETE', options: RequestOptions): Promise<T> {
  const { url, data, header = {}, showLoading = false, showError = true } = options
  const token = getToken()
  const payload = sanitizeRequestData(data)
  const fullUrl = url.startsWith('http') ? url : `${BASE_URL}${url}`

  if (showLoading) {
    Taro.showLoading({ title: '加载中…', mask: true })
  }

  let toastMessage: string | undefined

  try {
    const res = await Taro.request<ApiResult<T>>({
      url: fullUrl,
      method,
      data: payload,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...header
      }
    })

    const { statusCode } = res
    const body = res.data

    if (statusCode === 401 || (body && typeof body === 'object' && body.code === 40100)) {
      clearAuthState()
      toastMessage = '请先登录'
      throw new Error('Unauthorized')
    }

    if (statusCode < 200 || statusCode >= 300) {
      const message = toErrorText(body?.message) || `请求失败 (${statusCode})`
      toastMessage = message
      throw new Error(message)
    }

    if (body && typeof body === 'object' && 'code' in body && body.code !== 0) {
      if (body.code === 40100) {
        clearAuthState()
      }
      const message = toErrorText(body.message) || '请求失败'
      toastMessage = message
      throw new Error(message)
    }

    // 仅解包 data；成功时允许 data 为 null/空，勿回退成整个 ApiResult
    if (body && typeof body === 'object' && 'code' in body) {
      return body.data as T
    }
    return body as T
  } catch (err) {
    if (!toastMessage) {
      toastMessage = friendlyRequestError(toErrorText(err))
    } else {
      toastMessage = friendlyRequestError(toastMessage)
    }
    if (showError && toastMessage) {
      showToastAfterLoading(toastMessage)
    }
    throw err instanceof Error ? err : new Error(toastMessage || '请求失败')
  } finally {
    if (showLoading) {
      Taro.hideLoading()
    }
  }
}

export function get<T>(url: string, data?: Record<string, unknown>, options?: Omit<RequestOptions, 'url' | 'data'>) {
  return request<T>('GET', { url, data, ...options })
}

export function post<T>(url: string, data?: Record<string, unknown>, options?: Omit<RequestOptions, 'url' | 'data'>) {
  return request<T>('POST', { url, data, ...options })
}

export function put<T>(url: string, data?: Record<string, unknown>, options?: Omit<RequestOptions, 'url' | 'data'>) {
  return request<T>('PUT', { url, data, ...options })
}

export function del<T>(url: string, data?: Record<string, unknown>, options?: Omit<RequestOptions, 'url' | 'data'>) {
  return request<T>('DELETE', { url, data, ...options })
}

export default { get, post, put, delete: del, getToken, setToken, clearToken }
