import { View, Text, Image } from '@tarojs/components'
import Taro from '@tarojs/taro'
import type { Order } from '@/types'
import StatusBadge from '../StatusBadge'
import './OrderCard.scss'

interface OrderCardProps {
  order: Order
}

export default function OrderCard({ order }: OrderCardProps) {
  const firstItem = order.items?.[0]
  const itemSummary = (order.items?.length || 0) > 1
    ? `${firstItem?.dishName || '菜品'} 等 ${order.items.length} 道`
    : firstItem?.dishName || '预约菜品'

  const handleTap = () => {
    Taro.navigateTo({ url: `/pages/order/detail?id=${order.id}` })
  }

  return (
    <View className='order-card' onClick={handleTap}>
      <View className='order-card__header'>
        <Text className='order-card__no'>#{order.orderNo}</Text>
        <StatusBadge status={order.status} />
      </View>
      <View className='order-card__body'>
        {firstItem?.coverUrl && (
          <Image className='order-card__cover' src={firstItem.coverUrl} mode='aspectFill' />
        )}
        <View className='order-card__info'>
          <Text className='order-card__title'>{itemSummary}</Text>
          <Text className='order-card__time'>下单时间：{formatTime(order.createdAt)}</Text>
          {order.note && <Text className='order-card__guest'>备注：{order.note}</Text>}
        </View>
      </View>
    </View>
  )
}

function formatTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}
