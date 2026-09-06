import { View, Text } from '@tarojs/components'
import type { OrderStatus } from '@/types'
import './StatusBadge.scss'

const STATUS_MAP: Record<OrderStatus, { label: string; tone: string }> = {
  PENDING: { label: '待确认', tone: 'warm' },
  CONFIRMED: { label: '已确认', tone: 'mint' },
  PREPARING: { label: '制作中', tone: 'cooking' },
  READY: { label: '可以取餐', tone: 'ready' },
  COMPLETED: { label: '已完成', tone: 'done' },
  CANCELLED: { label: '已取消', tone: 'muted' }
}

interface StatusBadgeProps {
  status: OrderStatus | string
}

export default function StatusBadge({ status }: StatusBadgeProps) {
  const key = String(status).toUpperCase() as OrderStatus
  const meta = STATUS_MAP[key] || { label: String(status), tone: 'muted' }

  return (
    <View className={`status-badge status-badge--${meta.tone}`}>
      <Text>{meta.label}</Text>
    </View>
  )
}
