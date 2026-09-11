import { View, Text } from '@tarojs/components'
import { useProductStore } from '@/stores/productStore'
import './Loading.scss'

interface LoadingProps {
  text?: string
  fullscreen?: boolean
}

export default function Loading({ text, fullscreen = false }: LoadingProps) {
  const product = useProductStore((s) => s.product)
  const displayText = text ?? (product === 'health' ? '加载中…' : '厨房忙碌中…')

  return (
    <View className={`loading ${fullscreen ? 'loading--fullscreen' : ''}`}>
      <View className='loading__spinner'>
        <View className='loading__dot loading__dot--1' />
        <View className='loading__dot loading__dot--2' />
        <View className='loading__dot loading__dot--3' />
      </View>
      <Text className='loading__text'>{displayText}</Text>
    </View>
  )
}
