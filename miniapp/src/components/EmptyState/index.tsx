import { View, Text } from '@tarojs/components'
import './EmptyState.scss'

interface EmptyStateProps {
  emoji?: string
  title: string
  description?: string
  actionText?: string
  onAction?: () => void
}

export default function EmptyState({
  emoji = '🍳',
  title,
  description,
  actionText,
  onAction
}: EmptyStateProps) {
  return (
    <View className='empty-state'>
      <Text className='empty-state__emoji'>{emoji}</Text>
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
