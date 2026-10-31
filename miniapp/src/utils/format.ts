/** 秒转易读时长；不足 1 分钟按 1 分钟展示 */
export function formatDuration(seconds?: number | null): string {
  if (!seconds || seconds <= 0) return '未估时'
  const minutes = Math.max(1, Math.round(seconds / 60))
  if (minutes < 60) return `约 ${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest > 0 ? `约 ${hours} 小时 ${rest} 分` : `约 ${hours} 小时`
}

/** 步骤计时方式文案 */
export function timerTypeLabel(timerType?: string): string {
  switch (timerType) {
    case 'countdown':
      return '倒计时'
    case 'countup':
      return '正计时'
    default:
      return '不计时'
  }
}
