import {Image, Text, View} from '@tarojs/components'
import MiniIcon from '@/components/MiniIcon'
import './KitchenBindingCard.scss'

export interface KitchenBindingCardProps {
  kitchenName?: string
  statusText: string
  /** 厨房封面（后端）；无则橙色渐变底 */
  coverUrl?: string
  actionLabel: string
  onAction: () => void
  onClick?: () => void
}

/** 我的页 · 厨房绑定条：封面可选，行动按钮由页面注入 */
export default function KitchenBindingCard({
  kitchenName = '尚未绑定厨房',
  statusText,
  coverUrl,
  actionLabel,
  onAction,
  onClick
}: KitchenBindingCardProps) {
  return (
    <View className='kitchen-binding-card ck-pressable' onClick={onClick}>
      {coverUrl ? (
        <Image className='kitchen-binding-card__cover' src={coverUrl} mode='aspectFill' lazyLoad />
      ) : null}
      <View className='kitchen-binding-card__shade' />
      <View className='kitchen-binding-card__inner'>
        <View className='kitchen-binding-card__ico'>
          <MiniIcon name='kitchen' size='md' tone='primary' />
        </View>
        <View className='kitchen-binding-card__body'>
          <Text className='kitchen-binding-card__name'>{kitchenName}</Text>
          <Text className='kitchen-binding-card__status'>{statusText}</Text>
        </View>
        <Text
          className='kitchen-binding-card__go'
          onClick={(e) => {
            e.stopPropagation?.()
            onAction()
          }}
        >
          {actionLabel}
        </Text>
      </View>
    </View>
  )
}
