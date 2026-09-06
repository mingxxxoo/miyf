import { View, Text } from '@tarojs/components'
import './Loading.scss'

interface LoadingProps {
  text?: string
  fullscreen?: boolean
}

export default function Loading({ text = '厨房忙碌中…', fullscreen = false }: LoadingProps) {
  return (
    <View className={`loading ${fullscreen ? 'loading--fullscreen' : ''}`}>
      <View className='loading__spinner'>
        <View className='loading__dot loading__dot--1' />
        <View className='loading__dot loading__dot--2' />
        <View className='loading__dot loading__dot--3' />
      </View>
      <Text className='loading__text'>{text}</Text>
    </View>
  )
}
