import {Image, Text, View} from '@tarojs/components'
import Taro from '@tarojs/taro'
import type {Dish} from '@/types'
import MiniIcon from '@/components/MiniIcon'
import QtyStepper from '@/components/QtyStepper'
import './DishCard.scss'

export type DishCardVariant = 'cover' | 'list' | 'grid'

interface DishCardProps {
  dish: Dish
  /** @deprecated 用 variant="grid" */
  compact?: boolean
  /** cover=大图叠加（菜品页）；list=图文列表（首页）；grid=双列 */
  variant?: DishCardVariant
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

function stockDetail(dish: Dish): string | null {
  if (dish.stockType === 'LIMITED') {
    const n = dish.stock ?? 0
    return `今日还剩 ${n} 份`
  }
  if (dish.stockType === 'UNLIMITED') return '不限量'
  return null
}

function metaLine(dish: Dish): string {
  const parts: string[] = []
  if (dish.subtitle) parts.push(dish.subtitle)
  else if (dish.description) parts.push(dish.description.slice(0, 18))
  else if (dish.tags?.length) parts.push(dish.tags.slice(0, 2).join(' · '))
  else if (dish.categoryName) parts.push(dish.categoryName)
  return parts.join(' · ')
}

export default function DishCard({
  dish,
  compact = false,
  variant,
  quantity = 0,
  onAdd,
  onDec
}: DishCardProps) {
  const mode: DishCardVariant = variant || (compact ? 'grid' : 'list')
  const stock = stockLabel(dish)
  const soldOut = Boolean(stock?.soldOut)
  const qty = Math.max(0, quantity)

  const handleTap = () => {
    Taro.navigateTo({ url: `/pages/dish/detail?id=${dish.id}` })
  }

  const handleInc = () => {
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

  const handleDec = () => {
    if (onDec) onDec(dish)
  }

  const stepper = (
    <QtyStepper
      value={qty}
      disabled={soldOut}
      size={mode === 'cover' ? 'sm' : 'md'}
      showZeroAsAdd
      onInc={handleInc}
      onDec={handleDec}
    />
  )

  if (mode === 'cover') {
    const detail = stockDetail(dish)
    return (
      <View
        className={`dish-card dish-card--cover ${soldOut ? 'dish-card--soldout' : ''} ck-pressable`}
        onClick={handleTap}
      >
        <View className='dish-card__media'>
          {dish.coverUrl ? (
            <Image className='dish-card__media-img' src={dish.coverUrl} mode='aspectFill' lazyLoad />
          ) : (
            <View className='dish-card__media-empty'>
              <MiniIcon name='dish' size='lg' tone='muted' />
            </View>
          )}
          <View className='dish-card__fade' />
          <View className='dish-card__badges'>
            {dish.recommend ? <Text className='dish-card__tag'>荐</Text> : null}
            {stock?.soldOut ? (
              <Text className='dish-card__tag dish-card__tag--gone'>售罄</Text>
            ) : stock?.hot ? (
              <Text className='dish-card__tag dish-card__tag--warn'>仅剩 {dish.stock}</Text>
            ) : null}
            {dish.prepMinutes != null ? (
              <Text className='dish-card__time'>约 {dish.prepMinutes}′</Text>
            ) : (
              <View />
            )}
          </View>
          <View className='dish-card__info'>
            <View className='dish-card__copy'>
              <Text className='dish-card__cover-name'>{dish.name}</Text>
              {metaLine(dish) ? (
                <Text className='dish-card__cover-meta'>{metaLine(dish)}</Text>
              ) : null}
              {detail ? (
                soldOut || dish.stockType !== 'LIMITED' ? (
                  <Text className='dish-card__cover-stock'>{detail}</Text>
                ) : (
                  <Text className='dish-card__cover-stock'>
                    今日还剩{' '}
                    <Text className='dish-card__cover-stock-n'>{dish.stock}</Text> 份
                  </Text>
                )
              ) : null}
            </View>
            {stepper}
          </View>
        </View>
      </View>
    )
  }

  if (mode === 'grid') {
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
            <MiniIcon name='dish' size='lg' tone='muted' className='dish-card__grid-emoji' />
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
            {stepper}
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
          <MiniIcon name='dish' size='lg' tone='muted' className='dish-card__big-emoji' />
        )}
        {dish.recommend ? <Text className='dish-card__hot-tag'>热门</Text> : null}
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
          {stepper}
        </View>
      </View>
    </View>
  )
}
