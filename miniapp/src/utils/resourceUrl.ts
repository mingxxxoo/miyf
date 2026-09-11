/**
 * 后端读取接口为 GET /r/{fileId}。
 * 纠正 /media/r/...（baseUrl 误带 /media）与 /r/{id}.ext，保留 exp/sig。
 * 相对路径补全为绝对 URL，供小程序 Image 使用。
 */

const API_BASE = (process.env.TARO_APP_API_BASE || 'https://www.miyf.cn').replace(/\/$/, '')

export function toResourceUrl(url?: string | null): string | undefined {
  if (url == null) return undefined
  const trimmed = String(url).trim()
  if (!trimmed) return undefined

  const m = trimmed.match(
    /(?:https?:\/\/[^/?#]+)?(?:\/media)?(\/r\/\d+)(?:\.(?:jpe?g|png|gif|webp|bmp|svg))?([?#].*)?$/i
  )
  if (!m) return trimmed

  return m[1] + (m[2] ?? '')
}

/** 返回可供 Image 使用的绝对地址 */
export function toAbsoluteResourceUrl(url?: string | null): string | undefined {
  const path = toResourceUrl(url)
  if (!path) return undefined
  if (/^https?:\/\//i.test(path)) return path
  if (path.startsWith('/')) return `${API_BASE}${path}`
  return path
}

export function toResourceUrlOrEmpty(url?: string | null): string {
  return toAbsoluteResourceUrl(url) ?? ''
}
