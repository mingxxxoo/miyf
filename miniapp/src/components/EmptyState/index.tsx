import {Text, View} from '@tarojs/components'
import MiniIcon, {type MiniIconName} from '@/components/MiniIcon'
import {useProductStore} from '@/stores/productStore'
import './EmptyState.scss'

interface EmptyStateProps {
  /** @deprecated 优先用 icon */
  emoji?: string
  icon?: MiniIconName
  title: string
  description?: string
  actionText?: string
  onAction?: () => void
}

export default function EmptyState({
  emoji,
  icon,
  title,
  description,
  actionText,
  onAction
}: EmptyStateProps) {
  const product = useProductStore((s) => s.product)
  const resolvedIcon: MiniIconName =
    icon || (product === 'health' ? 'heart' : emoji ? 'empty' : 'dish')

  return (
    <View className='empty-state'>
      <View className='empty-state__ico'>
        <MiniIcon name={resolvedIcon} size='lg' tone='bare' />
      </View>
      <Text className='empty-state__title'>{title}</Text>
      {description && <Text className='empty-state__desc'>{description}</Text>}
      {actionText && onAction && (
        <View className='empty-state__action' onClick={onAction}>
          <Text>{actionText}</Text>
        </View>
      )}
    </View>
  )
}
