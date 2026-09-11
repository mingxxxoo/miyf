import { View, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { useMemo } from 'react'
import type { HealthTrendPoint } from '@/api/health'
import './TrendSpark.scss'

interface TrendSparkProps {
  points: HealthTrendPoint[]
  unit?: string
  accent?: 'kitchen' | 'health'
}

function sliceDateLabel(raw?: string): string {
  if (!raw) return ''
  const normalized = raw.replace('T', ' ')
  const m = normalized.match(/(\d{4})-(\d{2})-(\d{2})/)
  if (m) return `${m[2]}-${m[3]}`
  const d = new Date(raw)
  if (!Number.isNaN(d.getTime())) {
    const pad = (n: number) => String(n).padStart(2, '0')
    return `${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  }
  return normalized.slice(5, 10)
}

export default function TrendSpark({
  points,
  unit,
  accent = 'health'
}: TrendSparkProps) {
  const { bars, startLabel, endLabel } = useMemo(() => {
    const values = points
      .map((p) => (typeof p.value === 'number' ? p.value : null))
      .filter((v): v is number => v != null)
    if (!values.length) {
      return {
        bars: [] as { key: string; height: number; value: number; time?: string }[],
        startLabel: '',
        endLabel: ''
      }
    }
    const min = Math.min(...values)
    const max = Math.max(...values)
    const span = max - min || 1
    const sliced = points.slice(-12)
    const mapped = sliced.map((p, i) => {
      const v = typeof p.value === 'number' ? p.value : min
      const ratio = (v - min) / span
      const height = Math.round(28 + ratio * 72)
      return { key: `${p.measuredTime}-${i}`, height, value: v, time: p.measuredTime }
    })
    const first = sliced[0]
    const last = sliced[sliced.length - 1]
    return {
      bars: mapped,
      startLabel: sliceDateLabel(first?.measuredTime),
      endLabel: sliceDateLabel(last?.measuredTime)
    }
  }, [points])

  const onBarTap = (value: number) => {
    const u = unit ? ` ${unit}` : ''
    Taro.showToast({ title: `${value}${u}`, icon: 'none' })
  }

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
          <View
            key={b.key}
            className='trend-spark__col'
            onClick={() => onBarTap(b.value)}
          >
            <View className='trend-spark__bar' style={{ height: `${b.height}rpx` }} />
          </View>
        ))}
      </View>
      {(startLabel || endLabel) && (
        <View className='trend-spark__dates'>
          <Text className='trend-spark__date'>{startLabel}</Text>
          <Text className='trend-spark__date'>{endLabel}</Text>
        </View>
      )}
      {unit ? <Text className='trend-spark__unit'>单位 {unit} · 点按查看数值</Text> : (
        <Text className='trend-spark__unit'>点按查看数值</Text>
      )}
    </View>
  )
}
