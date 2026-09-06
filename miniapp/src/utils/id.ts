/**
 * 雪花 / 后端 Long ID：统一按字符串处理，避免 JS 精度丢失。
 */
export function asId(value: unknown): string {
  if (value == null) return ''
  return String(value)
}

export function asOptionalId(value: unknown): string | undefined {
  if (value == null || value === '') return undefined
  return String(value)
}
