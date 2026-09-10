import { View, Text, Input, Button, Picker } from '@tarojs/components'
import Taro, { useDidShow } from '@tarojs/taro'
import { useMemo, useState } from 'react'
import EmptyState from '@/components/EmptyState'
import Loading from '@/components/Loading'
import {
  createMySample,
  fetchMyHealth,
  fetchMySamples,
  fetchMyTrend,
  METRIC_OPTIONS,
  metricLabel,
  removeMySample,
  type HealthSample,
  type HealthSubject,
  type HealthTrend
} from '@/api/health'
import { useUserStore } from '@/stores/userStore'
import './index.scss'

export default function HealthPage() {
  const { isLoggedIn, requireLogin } = useUserStore()
  const [loading, setLoading] = useState(true)
  const [subject, setSubject] = useState<HealthSubject | null>(null)
  const [samples, setSamples] = useState<HealthSample[]>([])
  const [trend, setTrend] = useState<HealthTrend | null>(null)
  const [metricIndex, setMetricIndex] = useState(0)
  const [value, setValue] = useState('')

  const metric = METRIC_OPTIONS[metricIndex]

  const latestText = useMemo(() => {
    if (!trend || trend.latest == null) return '暂无数据'
    const unit = trend.unit ? ` ${trend.unit}` : ''
    return `${trend.latest}${unit}`
  }, [trend])

  const load = async () => {
    setLoading(true)
    try {
      const me = await fetchMyHealth()
      setSubject(me)
      const code = METRIC_OPTIONS[metricIndex].code
      const [sampleList, trendData] = await Promise.all([
        fetchMySamples({ limit: 20 }),
        fetchMyTrend(code, 30)
      ])
      setSamples(sampleList)
      setTrend(trendData)
    } catch {
      // toast 已由 request 处理
    } finally {
      setLoading(false)
    }
  }

  useDidShow(() => {
    if (!isLoggedIn) {
      void requireLogin()
      return
    }
    void load()
  })

  const onMetricChange = async (e: { detail: { value: string | number } }) => {
    const next = Number(e.detail.value) || 0
    setMetricIndex(next)
    try {
      const trendData = await fetchMyTrend(METRIC_OPTIONS[next].code, 30)
      setTrend(trendData)
    } catch {
      // ignore
    }
  }

  const handleCreate = async () => {
    const num = Number(value)
    if (!Number.isFinite(num)) {
      Taro.showToast({ title: '请输入有效数值', icon: 'none' })
      return
    }
    try {
      await createMySample({
        metricCode: metric.code,
        valueNum: num,
        unit: metric.unit,
        measuredTime: new Date().toISOString()
      })
      setValue('')
      Taro.showToast({ title: '已记录', icon: 'success' })
      await load()
    } catch {
      // ignore
    }
  }

  const handleDelete = (id: string) => {
    Taro.showModal({
      title: '删除记录',
      content: '确认删除这条采样？',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await removeMySample(id)
          await load()
        } catch {
          // ignore
        }
      }
    })
  }

  if (!isLoggedIn) {
    return <Loading fullscreen text='正在前往登录…' />
  }

  if (loading && !subject) {
    return <Loading fullscreen text='加载健康数据…' />
  }

  return (
    <View className='health-page'>
      <View className='health-page__hero ck-card'>
        <Text className='health-page__title'>{subject?.displayName || '我的健康'}</Text>
        <Text className='health-page__sub'>仅查看与管理自己的体征数据</Text>
        <View className='health-page__stat'>
          <Text className='health-page__stat-label'>{metric.label}最近</Text>
          <Text className='health-page__stat-value'>{latestText}</Text>
        </View>
        {trend?.avg != null && (
          <Text className='health-page__meta'>
            近 {trend.pointCount} 次均值 {trend.avg}
            {trend.unit ? ` ${trend.unit}` : ''}
          </Text>
        )}
      </View>

      <View className='health-page__form ck-card'>
        <Text className='health-page__section'>手动录入</Text>
        <Picker mode='selector' range={METRIC_OPTIONS.map((m) => m.label)} value={metricIndex} onChange={onMetricChange}>
          <View className='health-page__picker'>
            <Text>指标</Text>
            <Text className='health-page__picker-value'>
              {metric.label} · {metric.unit}
            </Text>
          </View>
        </Picker>
        <Input
          className='health-page__input'
          type='digit'
          placeholder={`输入${metric.label}`}
          value={value}
          onInput={(e) => setValue(e.detail.value)}
        />
        <Button className='health-page__btn' onClick={handleCreate}>
          保存记录
        </Button>
      </View>

      <View className='health-page__list'>
        <Text className='health-page__section'>最近采样</Text>
        {!samples.length ? (
          <EmptyState title='还没有记录' description='先录入一条体重或心率吧' />
        ) : (
          samples.map((s) => (
            <View key={s.id} className='health-page__item ck-card' onLongPress={() => handleDelete(s.id)}>
              <View>
                <Text className='health-page__item-title'>
                  {metricLabel(s.metricCode)} {s.valueNum}
                  {s.unit ? ` ${s.unit}` : ''}
                </Text>
                <Text className='health-page__item-meta'>
                  {s.measuredTime?.replace('T', ' ').slice(0, 19)} · {s.providerCode || 'manual'}
                </Text>
              </View>
              <Text className='health-page__item-tip'>长按删除</Text>
            </View>
          ))
        )}
      </View>
    </View>
  )
}
