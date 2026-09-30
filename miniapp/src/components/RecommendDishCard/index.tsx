import {Image, Text, View} from '@tarojs/components'
import type {Dish} from '@/types'
import MiniIcon from '@/components/MiniIcon'
import './RecommendDishCard.scss'

export type RecommendDishCardVariant = 'hero' | 'mini'

export interface RecommendDishCardProps {
  dish: Dish
  variant?: RecommendDishCardVariant
  onOpen?: (dish: Dish) => void
  onAdd?: (dish: Dish) => void
}

function metaText(dish: Dish): string {
  const parts: string[] = []
  if (dish.stockType === 'LIMITED' && dish.stock != null) {
    parts.push(`还剩 ${dish.stock} 份`)
  } else if (dish.stockType === 'UNLIMITED') {
    parts.push('不限量')
  }
  if (dish.prepMinutes != null) parts.push(`约 ${dish.prepMinutes} 分钟`)
  if (dish.subtitle) parts.push(dish.subtitle)
  else if (dish.tags?.[0]) parts.push(dish.tags[0])
  return parts.join(' · ') || '厨师推荐'
}

function miniMeta(dish: Dish): string {
  const parts: string[] = []
  if (dish.stockType === 'LIMITED' && dish.stock != null) parts.push(`还剩 ${dish.stock}`)
  if (dish.prepMinutes != null) parts.push(`约 ${dish.prepMinutes}`)
  return parts.join(' · ') || '推荐'
}

/** 首页今日推荐：大图 / 双列小卡，封面仅用后端 coverUrl */
export default function RecommendDishCard({
  dish,
  variant = 'mini',
  onOpen,
  onAdd
}: RecommendDishCardProps) {
  const open = () => onOpen?.(dish)
  const add = (e: { stopPropagation?: () => void }) => {
    e.stopPropagation?.()
    onAdd?.(dish)
  }

  if (variant === 'hero') {
    return (
      <View className='rec-dish rec-dish--hero ck-pressable' onClick={open}>
        {dish.coverUrl ? (
          <Image className='rec-dish__bg' src={dish.coverUrl} mode='aspectFill' lazyLoad />
        ) : (
          <View className='rec-dish__bg rec-dish__bg--empty'>
            <MiniIcon name='dish' size='lg' tone='muted' />
          </View>
        )}
        <View className='rec-dish__shade' />
        <Text className='rec-dish__tag'>{dish.recommend ? '荐' : '新'}</Text>
        <View className='rec-dish__body'>
          <Text className='rec-dish__name'>{dish.name}</Text>
          <Text className='rec-dish__meta'>{metaText(dish)}</Text>
          <View className='rec-dish__foot'>
            <Text className='rec-dish__hint'>点击查看详情</Text>
            {onAdd ? (
              <Text className='rec-dish__add' onClick={add}>
                加入预约
              </Text>
            ) : null}
          </View>
        </View>
      </View>
    )
  }

  return (
    <View className='rec-dish rec-dish--mini ck-pressable' onClick={open}>
      {dish.coverUrl ? (
        <Image className='rec-dish__bg' src={dish.coverUrl} mode='aspectFill' lazyLoad />
      ) : (
        <View className='rec-dish__bg rec-dish__bg--empty rec-dish__bg--empty-sm'>
          <MiniIcon name='dish' size='md' tone='muted' />
        </View>
      )}
      <View className='rec-dish__shade rec-dish__shade--mini' />
      <View className='rec-dish__body rec-dish__body--mini'>
        <Text className='rec-dish__name rec-dish__name--mini'>{dish.name}</Text>
        <Text className='rec-dish__meta rec-dish__meta--mini'>{miniMeta(dish)}</Text>
      </View>
    </View>
  )
}
