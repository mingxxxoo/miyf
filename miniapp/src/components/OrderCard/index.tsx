import { View, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import type { Order } from '@/types'
import StatusBadge from '../StatusBadge'
import './OrderCard.scss'

interface OrderCardProps {
  order: Order
}

const FLOW_LABELS = ['已确认', '备餐', '制作中', '取餐']

function flowStep(status: string): number {
  if (status === 'PENDING') return 0
  if (status === 'CONFIRMED') return 1
  if (status === 'PREPARING') return 2
  if (status === 'READY' || status === 'COMPLETED') return 3
  return -1
}

function itemSummary(order: Order) {
  const items = order.items || []
  if (!items.length) return '预约菜品'
  return items.map((it) => `${it.dishName} ×${it.quantity}`).join(' · ')
}

export default function OrderCard({ order }: OrderCardProps) {
  const step = flowStep(order.status)
  const active =
    order.status !== 'COMPLETED' &&
    order.status !== 'CANCELLED' &&
    step >= 0

  const handleTap = () => {
    Taro.navigateTo({ url: `/pages/order/detail?id=${order.id}` })
  }

  return (
    <View className='order-card ck-pressable' onClick={handleTap}>
      <View className='order-card__header'>
        <View className='order-card__user'>
          <View className='order-card__avatar'>
            <Text>{(order.orderNo || '预').slice(-2, -1) || '预'}</Text>
          </View>
          <View>
            <Text className='order-card__title'>{itemSummary(order)}</Text>
            <Text className='order-card__no'>单号 #{order.orderNo}</Text>
          </View>
        </View>
        <StatusBadge status={order.status} />
      </View>

      {order.note ? <Text className='order-card__note'>{order.note}</Text> : null}

      {active ? (
        <>
          <View className='order-card__mini-flow'>
            {[0, 1, 2, 3].map((i) => (
              <View
                key={i}
                className={`order-card__mf ${
                  i < step ? 'order-card__mf--done' : i === step ? 'order-card__mf--cur' : ''
                }`}
              />
            ))}
          </View>
          <View className='order-card__mini-labels'>
            {FLOW_LABELS.map((label, i) => (
              <Text
                key={label}
                className={`order-card__mf-label ${i === step ? 'order-card__mf-label--cur' : ''}`}
              >
                {label}
              </Text>
            ))}
          </View>
        </>
      ) : null}

      <View className='order-card__foot'>
        <Text className='order-card__time'>🕐 {formatTime(order.createTime)}</Text>
        {order.status === 'COMPLETED' ? (
          <Text
            className='order-card__action'
            onClick={(e) => {
              e.stopPropagation?.()
              const first = order.items?.[0]
              if (first?.dishId) {
                Taro.navigateTo({
                  url: `/pages/comment/create?orderId=${order.id}&dishId=${first.dishId}&dishName=${encodeURIComponent(first.dishName || '')}`
                })
              } else {
                Taro.navigateTo({ url: `/pages/order/detail?id=${order.id}` })
              }
            }}
          >
            去评价
          </Text>
        ) : null}
      </View>
    </View>
  )
}

function formatTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}
