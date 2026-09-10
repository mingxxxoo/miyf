import { View, Text, Input, Button, ScrollView } from '@tarojs/components'
import Taro, { useDidShow, usePullDownRefresh } from '@tarojs/taro'
import { useMemo, useState } from 'react'
import EmptyState from '@/components/EmptyState'
import Loading from '@/components/Loading'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import TrendSpark from '@/components/TrendSpark'
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
import { PRODUCT_META, useProductStore } from '@/stores/productStore'
import { useUserStore } from '@/stores/userStore'
import './index.scss'

type Panel = 'overview' | 'record'

export default function HealthPage() {
  const { isLoggedIn, requireLogin } = useUserStore()
  const setProduct = useProductStore((s) => s.setProduct)
  const [loading, setLoading] = useState(true)
  const [subject, setSubject] = useState<HealthSubject | null>(null)
  const [samples, setSamples] = useState<HealthSample[]>([])
  const [trend, setTrend] = useState<HealthTrend | null>(null)
  const [metricIndex, setMetricIndex] = useState(0)
  const [value, setValue] = useState('')
  const [panel, setPanel] = useState<Panel>('overview')
  const [saving, setSaving] = useState(false)

  const metric = METRIC_OPTIONS[metricIndex]

  const latestText = useMemo(() => {
    if (!trend || trend.latest == null) return '—'
    const unit = trend.unit ? ` ${trend.unit}` : ''
    return `${trend.latest}${unit}`
  }, [trend])

  const filteredSamples = useMemo(() => {
    return samples.filter((s) => s.metricCode === metric.code)
  }, [samples, metric.code])

  const load = async () => {
    setLoading(true)
    try {
      const me = await fetchMyHealth()
      setSubject(me)
      const code = METRIC_OPTIONS[metricIndex].code
      const [sampleList, trendData] = await Promise.all([
        fetchMySamples({ limit: 30 }),
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
    setProduct('health')
    Taro.setNavigationBarTitle({ title: PRODUCT_META.health.brand })
    if (!isLoggedIn) {
      void requireLogin()
      return
    }
    void load()
  })

  usePullDownRefresh(async () => {
    try {
      await load()
    } finally {
      Taro.stopPullDownRefresh()
    }
  })

  const onMetricSelect = async (index: number) => {
    if (index === metricIndex) return
    setMetricIndex(index)
    try {
      Taro.vibrateShort({ type: 'light' })
    } catch {
      // ignore
    }
    try {
      const trendData = await fetchMyTrend(METRIC_OPTIONS[index].code, 30)
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
    setSaving(true)
    try {
      await createMySample({
        metricCode: metric.code,
        valueNum: num,
        unit: metric.unit,
        measuredTime: new Date().toISOString()
      })
      setValue('')
      Taro.showToast({ title: '已记录', icon: 'success' })
      setPanel('overview')
      await load()
    } catch {
      // ignore
    } finally {
      setSaving(false)
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
      <View className='health-page__top'>
        <ServiceSwitcher compact />
      </View>

      <View className='health-page__hero'>
        <Text className='health-page__brand'>健康服务</Text>
        <Text className='health-page__title'>{subject?.displayName || '我的健康'}</Text>
        <Text className='health-page__sub'>记录体征，看看自己的变化趋势</Text>

        <View className='health-page__stat-row'>
          <View className='health-page__stat-card'>
            <Text className='health-page__stat-label'>{metric.label}最近</Text>
            <Text className='health-page__stat-value'>{latestText}</Text>
          </View>
          <View className='health-page__stat-card'>
            <Text className='health-page__stat-label'>近 {trend?.pointCount || 0} 次均值</Text>
            <Text className='health-page__stat-value health-page__stat-value--sm'>
              {trend?.avg != null ? `${trend.avg}${trend.unit ? ` ${trend.unit}` : ''}` : '—'}
            </Text>
          </View>
        </View>
      </View>

      <ScrollView scrollX className='health-page__metrics' enhanced showScrollbar={false}>
        {METRIC_OPTIONS.map((m, i) => (
          <View
            key={m.code}
            className={`health-page__metric ck-pressable ${
              i === metricIndex ? 'health-page__metric--active' : ''
            }`}
            onClick={() => void onMetricSelect(i)}
          >
            <Text className='health-page__metric-label'>{m.label}</Text>
            <Text className='health-page__metric-unit'>{m.unit}</Text>
          </View>
        ))}
      </ScrollView>

      <View className='health-page__panels'>
        <View
          className={`health-page__panel-tab ${panel === 'overview' ? 'health-page__panel-tab--active' : ''}`}
          onClick={() => setPanel('overview')}
        >
          <Text>趋势概览</Text>
        </View>
        <View
          className={`health-page__panel-tab ${panel === 'record' ? 'health-page__panel-tab--active' : ''}`}
          onClick={() => setPanel('record')}
        >
          <Text>手动录入</Text>
        </View>
      </View>

      {panel === 'overview' ? (
        <View className='health-page__card ck-card'>
          <View className='health-page__card-head'>
            <Text className='health-page__section'>{metric.label}趋势</Text>
            <Text
              className='health-page__link'
              onClick={() => setPanel('record')}
            >
              去录入 →
            </Text>
          </View>
          <TrendSpark points={trend?.points || []} unit={trend?.unit || metric.unit} />
          {(trend?.min != null || trend?.max != null) && (
            <View className='health-page__range'>
              <Text>最低 {trend?.min ?? '—'}</Text>
              <Text>最高 {trend?.max ?? '—'}</Text>
            </View>
          )}
        </View>
      ) : (
        <View className='health-page__card ck-card'>
          <Text className='health-page__section'>录入 {metric.label}</Text>
          <Text className='health-page__hint'>单位 {metric.unit}，保存后出现在趋势与列表中</Text>
          <Input
            className='health-page__input'
            type='digit'
            placeholder={`输入${metric.label}数值`}
            value={value}
            onInput={(e) => setValue(e.detail.value)}
          />
          <Button
            className='ck-btn-health health-page__btn'
            loading={saving}
            onClick={() => void handleCreate()}
          >
            保存记录
          </Button>
        </View>
      )}

      <View className='health-page__list'>
        <Text className='health-page__section'>最近 · {metric.label}</Text>
        {!filteredSamples.length ? (
          <EmptyState
            emoji='💚'
            title='还没有这类记录'
            description={`先录入一条${metric.label}吧`}
            actionText='去录入'
            onAction={() => setPanel('record')}
          />
        ) : (
          filteredSamples.map((s) => (
            <View
              key={s.id}
              className='health-page__item ck-card ck-pressable'
              onLongPress={() => handleDelete(s.id)}
            >
              <View>
                <Text className='health-page__item-title'>
                  {metricLabel(s.metricCode)} {s.valueNum}
                  {s.unit ? ` ${s.unit}` : ''}
                </Text>
                <Text className='health-page__item-meta'>
                  {s.measuredTime?.replace('T', ' ').slice(0, 19)} ·{' '}
                  {s.providerCode === 'manual' || !s.providerCode ? '手动' : s.providerCode}
                </Text>
              </View>
              <Text className='health-page__item-tip'>长按删除</Text>
            </View>
          ))
        )}
      </View>

      <View className='health-page__dock'>
        <View
          className='health-page__dock-item ck-pressable'
          onClick={() => {
            setProduct('kitchen')
            Taro.switchTab({ url: '/pages/index/index' })
          }}
        >
          <Text className='health-page__dock-label'>厨房</Text>
        </View>
        <View className='health-page__dock-item health-page__dock-item--active'>
          <Text className='health-page__dock-label'>健康</Text>
        </View>
        <View
          className='health-page__dock-item ck-pressable'
          onClick={() => Taro.switchTab({ url: '/pages/user/index' })}
        >
          <Text className='health-page__dock-label'>我的</Text>
        </View>
      </View>
    </View>
  )
}
