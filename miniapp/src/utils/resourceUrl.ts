/**
 * 后端读取接口为 GET /r/{fileId}。
 * 纠正 /media/r/...（baseUrl 误带 /media）与 /r/{id}.ext，保留 exp/sig。
 */
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

export function toResourceUrlOrEmpty(url?: string | null): string {
  return toResourceUrl(url) ?? ''
}
