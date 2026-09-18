import { Text, View } from '@tarojs/components'
import './MiniIcon.scss'

export type MiniIconName =
  | 'dish'
  | 'recipe'
  | 'category'
  | 'order'
  | 'people'
  | 'ticket'
  | 'home'
  | 'trend'
  | 'kitchen'
  | 'spark'
  | 'basket'
  | 'check'
  | 'empty'

const GLYPHS: Record<MiniIconName, string> = {
  dish: '◒',
  recipe: '▤',
  category: '▦',
  order: '☷',
  people: '◎',
  ticket: '◇',
  home: '⌂',
  trend: '↗',
  kitchen: '⌁',
  spark: '✦',
  basket: '▽',
  check: '✓',
  empty: '○'
}

interface MiniIconProps {
  name: MiniIconName
  size?: 'sm' | 'md' | 'lg'
  tone?: 'default' | 'primary' | 'mint' | 'muted'
  className?: string
}

export default function MiniIcon({
  name,
  size = 'md',
  tone = 'default',
  className = ''
}: MiniIconProps) {
  return (
    <View className={`mini-icon mini-icon--${size} mini-icon--${tone} ${className}`.trim()}>
      <Text className='mini-icon__glyph'>{GLYPHS[name]}</Text>
    </View>
  )
}
