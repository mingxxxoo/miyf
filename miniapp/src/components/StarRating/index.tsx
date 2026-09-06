import { View, Text } from '@tarojs/components'
import './StarRating.scss'

interface StarRatingProps {
  value: number
  max?: number
  size?: 'sm' | 'md' | 'lg'
  readonly?: boolean
  onChange?: (value: number) => void
}

export default function StarRating({
  value,
  max = 5,
  size = 'md',
  readonly = false,
  onChange
}: StarRatingProps) {
  const stars = Array.from({ length: max }, (_, i) => i + 1)

  const handleTap = (star: number) => {
    if (!readonly && onChange) {
      onChange(star)
    }
  }

  return (
    <View className={`star-rating star-rating--${size}`}>
      {stars.map((star) => (
        <Text
          key={star}
          className={`star-rating__star ${star <= Math.round(value) ? 'star-rating__star--active' : ''} ${readonly ? '' : 'star-rating__star--interactive'}`}
          onClick={() => handleTap(star)}
        >
          ★
        </Text>
      ))}
    </View>
  )
}
