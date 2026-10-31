import type {CookingTaskStatus} from '@/types'

export const COOKING_STATUS_META: Record<CookingTaskStatus, { label: string; tone: string }> = {
  PENDING: { label: '待领取', tone: 'pending' },
  CLAIMED: { label: '已领取', tone: 'claimed' },
  PREPARING: { label: '备料中', tone: 'claimed' },
  COOKING: { label: '制作中', tone: 'cooking' },
  PAUSED: { label: '已暂停', tone: 'paused' },
  COMPLETED: { label: '已完成', tone: 'done' },
  CANCELLED: { label: '已取消', tone: 'cancelled' }
}

/** 时间展示：MM-DD HH:mm */
export function formatTaskTime(value?: string): string {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mi = String(date.getMinutes()).padStart(2, '0')
  return `${mm}-${dd} ${hh}:${mi}`
}

/** 秒转“x 分 y 秒” */
export function formatSeconds(seconds?: number): string {
  if (seconds == null || seconds < 0) return '—'
  if (seconds < 60) return `${seconds} 秒`
  const minutes = Math.floor(seconds / 60)
  const rest = seconds % 60
  return rest > 0 ? `${minutes} 分 ${rest} 秒` : `${minutes} 分钟`
}
