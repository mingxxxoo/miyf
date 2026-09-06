import { View, Text, Image } from '@tarojs/components'
import Taro from '@tarojs/taro'
import type { Dish } from '@/types'
import StarRating from '../StarRating'
import './DishCard.scss'

interface DishCardProps {
  dish: Dish
  compact?: boolean
}

export default function DishCard({ dish, compact = false }: DishCardProps) {
  const handleTap = () => {
    Taro.navigateTo({ url: `/pages/dish/detail?id=${dish.id}` })
  }

  return (
    <View className={`dish-card ${compact ? 'dish-card--compact' : ''}`} onClick={handleTap}>
      <Image className='dish-card__cover' src={dish.coverUrl} mode='aspectFill' />
      <View className='dish-card__body'>
        <Text className='dish-card__name'>{dish.name}</Text>
        {!compact && dish.description && (
          <Text className='dish-card__desc'>{dish.description}</Text>
        )}
        <View className='dish-card__meta'>
          {dish.rating != null ? (
            <StarRating value={dish.rating} size='sm' readonly />
          ) : (
            <Text className='dish-card__time'>暂无评分</Text>
          )}
          {dish.prepMinutes != null && (
            <Text className='dish-card__time'>约 {dish.prepMinutes} 分钟</Text>
          )}
        </View>
        {dish.tags && dish.tags.length > 0 && (
          <View className='dish-card__tags'>
            {dish.tags.slice(0, 3).map((tag) => (
              <Text key={tag} className='dish-card__tag'>{tag}</Text>
            ))}
          </View>
        )}
      </View>
    </View>
  )
}
