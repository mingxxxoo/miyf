import {Text, View} from '@tarojs/components'
import type {ChefOrder} from '@/api/kitchen'
import './ChefRecentOrderCard.scss'

export interface ChefRecentOrderCardProps {
  order: ChefOrder
  acting?: boolean
  /** 预约列表页展示流程节点 */
  showFlow?: boolean
  onAdvance?: (order: ChefOrder) => void
  onReject?: (order: ChefOrder) => void
  onOpen?: (order: ChefOrder) => void
}

const NEXT: Record<string, { status: string; label: string }> = {
  PENDING: { status: 'CONFIRMED', label: '确认接单' },
  CONFIRMED: { status: 'PREPARING', label: '开始备餐' },
  PREPARING: { status: 'READY', label: '可以取餐' },
  READY: { status: 'COMPLETED', label: '完成' }
}

const STATUS: Record<string, { text: string; tone: 'warn' | 'info' | 'ok' | 'mute' }> = {
  PENDING: { text: '待确认', tone: 'warn' },
  CONFIRMED: { text: '已确认', tone: 'info' },
  PREPARING: { text: '备餐中', tone: 'info' },
  READY: { text: '待取餐', tone: 'ok' },
  COMPLETED: { text: '已完成', tone: 'ok' },
  CANCELLED: { text: '已取消', tone: 'mute' }
}

const FLOW = [
  { key: 'PENDING', label: '确认' },
  { key: 'PREPARING', label: '备餐' },
  { key: 'READY', label: '取餐' },
  { key: 'COMPLETED', label: '完成' }
]

function pad(n: number) {
  return String(n).padStart(2, '0')
}

function dayLabel(iso?: string) {
  if (!iso) return ''
  const today = new Date()
  const todayStr = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`
  const t = new Date()
  t.setDate(t.getDate() + 1)
  const tomorrowStr = `${t.getFullYear()}-${pad(t.getMonth() + 1)}-${pad(t.getDate())}`
  if (iso === todayStr) return '今天'
  if (iso === tomorrowStr) return '明天'
  const parts = iso.split('-')
  if (parts.length >= 3) return `${Number(parts[1])}月${Number(parts[2])}日`
  return iso
}

function relativeTime(value?: string) {
  if (!value) return ''
  const t = new Date(value).getTime()
  if (Number.isNaN(t)) return ''
  const diff = Date.now() - t
  const m = Math.floor(diff / 60000)
  if (m < 1) return '刚刚提交'
  if (m < 60) return `${m} 分钟前提交`
  const h = Math.floor(m / 60)
  if (h < 24) return `${h} 小时前提交`
  return ''
}

function flowIndex(status: string) {
  if (status === 'PENDING' || status === 'CONFIRMED') return 0
  if (status === 'PREPARING') return 1
  if (status === 'READY') return 2
  if (status === 'COMPLETED') return 3
  return -1
}

function mealTypeLabel(value?: string) {
  if (!value) return ''
  const map: Record<string, string> = {
    BREAKFAST: '早餐',
    LUNCH: '午餐',
    DINNER: '晚餐',
    SNACK: '加餐',
    早餐: '早餐',
    午餐: '午餐',
    晚餐: '晚餐',
    加餐: '加餐'
  }
  return map[value] || map[value.toUpperCase()] || value
}

/** 厨师端预约卡：餐次优先、留白疏朗 */
export default function ChefRecentOrderCard({
  order,
  acting = false,
  showFlow = false,
  onAdvance,
  onReject,
  onOpen
}: ChefRecentOrderCardProps) {
  const next = NEXT[order.status]
  const status = STATUS[order.status] || STATUS.PENDING
  const nick = order.userNickname || '厨房朋友'
  const actionable = Boolean(next)
  const day = dayLabel(order.mealDate)
  const time = order.mealTime || ''
  const mealType = mealTypeLabel(order.mealType)
  const guests = order.guestCount ? `${order.guestCount} 人` : ''
  const submitted = relativeTime(order.createTime)
  const items = order.items || []
  const fi = flowIndex(order.status)
  const mealTitle = [day, time].filter(Boolean).join(' ') || '时间待定'

  return (
    <View
      className={`chef-book chef-book--${status.tone}${actionable ? ' chef-book--live' : ''}`}
      onClick={() => onOpen?.(order)}
    >
      <View className='chef-book__head'>
        <View className='chef-book__when'>
          <Text className='chef-book__when-main'>{mealTitle}</Text>
          {(mealType || guests) && (
            <Text className='chef-book__when-sub'>
              {[mealType, guests].filter(Boolean).join(' · ')}
            </Text>
          )}
        </View>
        <Text className={`chef-book__status chef-book__status--${status.tone}`}>{status.text}</Text>
      </View>

      <View className='chef-book__who'>
        <Text className='chef-book__who-name'>{nick}</Text>
        {submitted ? <Text className='chef-book__who-time'>{submitted}</Text> : null}
      </View>

      {showFlow && fi >= 0 ? (
        <View className='chef-book__flow' onClick={(e) => e.stopPropagation?.()}>
          {FLOW.map((step, i) => (
            <View key={step.key} className='chef-book__flow-item'>
              <View
                className={`chef-book__flow-dot${
                  i < fi ? ' is-done' : i === fi ? ' is-cur' : ''
                }`}
              >
                <Text>{i < fi ? '✓' : i + 1}</Text>
              </View>
              <Text
                className={`chef-book__flow-label${i === fi ? ' is-cur' : ''}`}
              >
                {step.label}
              </Text>
              {i < FLOW.length - 1 ? (
                <View className={`chef-book__flow-line${i < fi ? ' is-done' : ''}`} />
              ) : null}
            </View>
          ))}
        </View>
      ) : null}

      <View className='chef-book__menu'>
        {items.length === 0 ? (
          <Text className='chef-book__menu-empty'>暂无菜品</Text>
        ) : (
          items.map((it) => (
            <View key={`${it.dishId}-${it.dishName}`} className='chef-book__dish'>
              <Text className='chef-book__dish-dot'>·</Text>
              <Text className='chef-book__dish-name'>{it.dishName}</Text>
              <Text className='chef-book__dish-qty'>×{it.quantity}</Text>
            </View>
          ))
        )}
      </View>

      {order.remark ? (
        <Text className='chef-book__remark'>备注 · {order.remark}</Text>
      ) : null}

      {actionable ? (
        <View className='chef-book__actions' onClick={(e) => e.stopPropagation?.()}>
          {order.status === 'PENDING' && onReject ? (
            <View
              className={`chef-book__btn chef-book__btn--ghost${acting ? ' is-off' : ''}`}
              onClick={() => !acting && onReject(order)}
            >
              <Text>驳回</Text>
            </View>
          ) : (
            <View className='chef-book__btn-spacer' />
          )}
          {onAdvance ? (
            <View
              className={`chef-book__btn chef-book__btn--primary${acting ? ' is-off' : ''}`}
              onClick={() => !acting && onAdvance(order)}
            >
              <Text>{acting ? '处理中…' : next!.label}</Text>
            </View>
          ) : null}
        </View>
      ) : null}
    </View>
  )
}
