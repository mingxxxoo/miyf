import { View, Text } from '@tarojs/components'
import { useMemo } from 'react'
import type { HealthTrendPoint } from '@/api/health'
import './TrendSpark.scss'

interface TrendSparkProps {
  points: HealthTrendPoint[]
  unit?: string
  accent?: 'kitchen' | 'health'
}

export default function TrendSpark({
  points,
  unit,
  accent = 'health'
}: TrendSparkProps) {
  const bars = useMemo(() => {
    const values = points
      .map((p) => (typeof p.value === 'number' ? p.value : null))
      .filter((v): v is number => v != null)
    if (!values.length) return []
    const min = Math.min(...values)
    const max = Math.max(...values)
    const span = max - min || 1
    // 最多展示最近 12 个点，避免过密
    const sliced = points.slice(-12)
    return sliced.map((p, i) => {
      const v = typeof p.value === 'number' ? p.value : min
      const ratio = (v - min) / span
      const height = Math.round(28 + ratio * 72)
      return { key: `${p.measuredTime}-${i}`, height, value: v }
    })
  }, [points])

  if (!bars.length) {
    return (
      <View className='trend-spark trend-spark--empty'>
        <Text className='trend-spark__empty-text'>暂无趋势，先记一条吧</Text>
      </View>
    )
  }

  return (
    <View className={`trend-spark trend-spark--${accent}`}>
      <View className='trend-spark__bars'>
        {bars.map((b) => (
          <View key={b.key} className='trend-spark__col'>
            <View className='trend-spark__bar' style={{ height: `${b.height}rpx` }} />
          </View>
        ))}
      </View>
      {unit ? <Text className='trend-spark__unit'>单位 {unit}</Text> : null}
    </View>
  )
}
