import { View, Text, Image } from '@tarojs/components'
import Taro from '@tarojs/taro'
import type { Dish } from '@/types'
import './DishCard.scss'

interface DishCardProps {
  dish: Dish
  compact?: boolean
  quantity?: number
  onAdd?: (dish: Dish) => void
  onDec?: (dish: Dish) => void
}

function stockLabel(dish: Dish): { text: string; hot?: boolean; soldOut?: boolean } | null {
  if (dish.stockType === 'LIMITED') {
    const n = dish.stock ?? 0
    if (n <= 0) return { text: '今日售罄', soldOut: true }
    return { text: `剩 ${n} 份`, hot: n <= 8 }
  }
  if (dish.stockType === 'UNLIMITED') return { text: '不限量' }
  return null
}

export default function DishCard({
  dish,
  compact = false,
  quantity = 0,
  onAdd,
  onDec
}: DishCardProps) {
  const stock = stockLabel(dish)
  const soldOut = Boolean(stock?.soldOut)
  const qty = Math.max(0, quantity)

  const handleTap = () => {
    Taro.navigateTo({ url: `/pages/dish/detail?id=${dish.id}` })
  }

  const handleAdd = (e: { stopPropagation?: () => void }) => {
    e.stopPropagation?.()
    if (soldOut) {
      Taro.showToast({ title: '今日已约满', icon: 'none' })
      return
    }
    if (onAdd) {
      onAdd(dish)
      return
    }
    Taro.navigateTo({ url: `/pages/dish/detail?id=${dish.id}` })
  }

  const handleDec = (e: { stopPropagation?: () => void }) => {
    e.stopPropagation?.()
    if (onDec) onDec(dish)
  }

  const qtyControl = (
    <View
      className={`dish-card__qty ${soldOut ? 'dish-card__qty--disabled' : ''}`}
      onClick={(e) => e.stopPropagation?.()}
    >
      {qty > 0 ? (
        <>
          <View className='dish-card__qty-btn' onClick={handleDec}>
            <Text>−</Text>
          </View>
          <Text className='dish-card__qty-num'>{qty}</Text>
          <View
            className={`dish-card__qty-btn dish-card__qty-btn--plus ${
              soldOut ? 'is-disabled' : ''
            }`}
            onClick={handleAdd}
          >
            <Text>＋</Text>
          </View>
        </>
      ) : (
        <View
          className={`dish-card__add ${compact ? '' : 'dish-card__add--lg'} ${
            soldOut ? 'dish-card__add--disabled' : ''
          }`}
          onClick={handleAdd}
        >
          <Text>＋</Text>
        </View>
      )}
    </View>
  )

  if (compact) {
    return (
      <View
        className={`dish-card dish-card--grid ${soldOut ? 'dish-card--soldout' : ''} ck-pressable`}
        onClick={handleTap}
      >
        <View className='dish-card__grid-cover'>
          {dish.coverUrl ? (
            <Image
              className='dish-card__grid-img'
              src={dish.coverUrl}
              mode='aspectFill'
              lazyLoad
            />
          ) : (
            <Text className='dish-card__grid-emoji'>🍽️</Text>
          )}
          {stock ? (
            <Text
              className={`dish-card__stock-badge ${stock.hot ? 'dish-card__stock-badge--hot' : ''}`}
            >
              {stock.text}
            </Text>
          ) : null}
          {soldOut ? (
            <View className='dish-card__soldout-mask'>
              <Text className='dish-card__soldout-tag'>今日售罄</Text>
            </View>
          ) : null}
        </View>
        <View className='dish-card__grid-body'>
          <Text className='dish-card__grid-name'>{dish.name}</Text>
          <View className='dish-card__grid-foot'>
            <Text className='dish-card__grid-rate'>
              {dish.rating != null ? `★${dish.rating}` : '新菜'}
            </Text>
            {qtyControl}
          </View>
        </View>
      </View>
    )
  }

  return (
    <View
      className={`dish-card dish-card--big ${soldOut ? 'dish-card--soldout' : ''} ck-pressable`}
      onClick={handleTap}
    >
      <View className='dish-card__big-cover'>
        {dish.coverUrl ? (
          <Image
            className='dish-card__big-img'
            src={dish.coverUrl}
            mode='aspectFill'
            lazyLoad
          />
        ) : (
          <Text className='dish-card__big-emoji'>🥘</Text>
        )}
        {dish.recommend ? (
          <Text className='dish-card__hot-tag'>🔥 热门</Text>
        ) : null}
        {soldOut ? (
          <View className='dish-card__soldout-mask'>
            <Text className='dish-card__soldout-tag'>今日售罄</Text>
          </View>
        ) : null}
      </View>
      <View className='dish-card__big-body'>
        <Text className='dish-card__big-name'>{dish.name}</Text>
        {dish.description ? (
          <Text className='dish-card__big-desc'>{dish.description}</Text>
        ) : null}
        <View className='dish-card__big-foot'>
          <View className='dish-card__big-info'>
            {dish.rating != null ? (
              <Text className='dish-card__big-star'>★{dish.rating}</Text>
            ) : null}
            {stock && !soldOut ? <Text>{stock.text}</Text> : null}
            {dish.prepMinutes != null ? <Text>约 {dish.prepMinutes} 分钟</Text> : null}
          </View>
          {qtyControl}
        </View>
      </View>
    </View>
  )
}
