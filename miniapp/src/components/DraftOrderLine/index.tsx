import {Image, Text, View} from '@tarojs/components'
import MiniIcon from '@/components/MiniIcon'
import QtyStepper from '@/components/QtyStepper'
import './DraftOrderLine.scss'

export interface DraftOrderLineProps {
  dishId: string
  dishName: string
  /** 后端菜品封面绝对 URL；无则占位图标 */
  coverUrl?: string
  quantity: number
  subtitle?: string
  /** 浮层清单用更紧凑行高 */
  compact?: boolean
  showDelete?: boolean
  onInc: () => void
  onDec: () => void
  onRemove?: () => void
}

/** 预约草稿单行：封面来自后端 coverUrl，数量步进复用 QtyStepper */
export default function DraftOrderLine({
  dishName,
  coverUrl,
  quantity,
  subtitle,
  compact = false,
  showDelete = false,
  onInc,
  onDec,
  onRemove
}: DraftOrderLineProps) {
  return (
    <View className={`draft-order-line${compact ? ' draft-order-line--compact' : ''}`}>
      {coverUrl ? (
        <Image className='draft-order-line__cover' src={coverUrl} mode='aspectFill' lazyLoad />
      ) : (
        <View className='draft-order-line__cover draft-order-line__cover--empty'>
          <MiniIcon name='dish' size='sm' tone='muted' />
        </View>
      )}
      <View className='draft-order-line__body'>
        <Text className='draft-order-line__name'>{dishName}</Text>
        {subtitle ? <Text className='draft-order-line__sub'>{subtitle}</Text> : null}
        {compact ? (
          <Text className='draft-order-line__qty-hint'>×{quantity}</Text>
        ) : null}
      </View>
      <QtyStepper
        value={quantity}
        size='sm'
        showZeroAsAdd={false}
        onInc={onInc}
        onDec={onDec}
      />
      {showDelete && onRemove ? (
        <View
          className='draft-order-line__del'
          onClick={(e) => {
            e.stopPropagation?.()
            onRemove()
          }}
        >
          <Text className='draft-order-line__del-x'>×</Text>
        </View>
      ) : null}
    </View>
  )
}
