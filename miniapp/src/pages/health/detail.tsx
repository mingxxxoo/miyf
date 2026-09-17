import { View, Text } from '@tarojs/components'
import Taro, { useDidShow, usePullDownRefresh, useRouter } from '@tarojs/taro'
import { useMemo, useState } from 'react'
import EmptyState from '@/components/EmptyState'
import Loading from '@/components/Loading'
import TrendSpark from '@/components/TrendSpark'
import {
  assessMetricPoint,
  assessMetricValue,
  displayMetricValue,
  fetchMySamples,
  fetchMyTrend,
  formatMeasuredTime,
  formatStatValue,
  metricLabel,
  metricUnit,
  METRIC_OPTIONS,
  providerLabel,
  type HealthSample,
  type HealthTrend,
  type HealthTrendPoint,
  type MetricAlert
} from '@/api/health'
import { useAuthGuard } from '@/hooks/useAuthGuard'
import { useProductStore } from '@/stores/productStore'
import './detail.scss'

const RANGE_OPTIONS = [
  { days: 7, label: '7 天' },
  { days: 30, label: '30 天' },
  { days: 90, label: '90 天' }
] as const

function resolveMetric(raw?: string): string {
  const code = decodeURIComponent(raw || '').trim()
  if (METRIC_OPTIONS.some((m) => m.code === code)) return code
  return METRIC_OPTIONS[0].code
}

export default function HealthDetailPage() {
  const router = useRouter()
  const { isLoggedIn, bootstrapping } = useAuthGuard()
  const setProduct = useProductStore((s) => s.setProduct)
  const metricCode = resolveMetric(router.params.metric)
  const label = metricLabel(metricCode)
  const unit = metricUnit(metricCode)

  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const [rangeDays, setRangeDays] = useState(30)
  const [samples, setSamples] = useState<HealthSample[]>([])
  const [trend, setTrend] = useState<HealthTrend | null>(null)

  const series = useMemo(
    () => samples.map((s) => s.valueNum).filter((n) => Number.isFinite(n)),
    [samples]
  )

  const rows = useMemo(() => {
    return samples.map((s) => ({
      sample: s,
      alert: assessMetricPoint(metricCode, s.valueNum, series)
    }))
  }, [samples, metricCode, series])

  const alerts = useMemo(
    () => rows.filter((r): r is { sample: HealthSample; alert: MetricAlert } => Boolean(r.alert)),
    [rows]
  )

  const displayPoints = useMemo((): HealthTrendPoint[] => {
    return (trend?.points || []).map((p) => ({
      ...p,
      value: p.value == null ? p.value : displayMetricValue(metricCode, p.value)
    }))
  }, [trend?.points, metricCode])

  const latestAlert = useMemo(() => {
    if (trend?.latest == null) return null
    return assessMetricValue(metricCode, trend.latest)
  }, [trend?.latest, metricCode])

  const load = async (days = rangeDays) => {
    setLoading(true)
    setLoadError(false)
    try {
      const [sampleList, trendData] = await Promise.all([
        fetchMySamples({ metricCode, limit: 50 }),
        fetchMyTrend(metricCode, days)
      ])
      setSamples(sampleList)
      setTrend(trendData)
    } catch {
      setLoadError(true)
      setSamples([])
      setTrend(null)
    } finally {
      setLoading(false)
    }
  }

  const onRangeChange = (days: number) => {
    if (days === rangeDays) return
    setRangeDays(days)
    void load(days)
  }

  useDidShow(() => {
    setProduct('health')
    Taro.setNavigationBarTitle({ title: label })
    if (bootstrapping || !isLoggedIn) return
    void load()
  })

  usePullDownRefresh(async () => {
    try {
      if (!bootstrapping && isLoggedIn) await load()
    } finally {
      Taro.stopPullDownRefresh()
    }
  })

  if (bootstrapping || !isLoggedIn) {
    return <Loading fullscreen text='正在登录…' />
  }

  if (loading && !trend && !samples.length) {
    return <Loading fullscreen text='加载明细…' />
  }

  if (loadError && !samples.length) {
    return (
      <View className='health-detail'>
        <EmptyState
          title='加载失败'
          description='无法获取该指标明细，请稍后重试'
          actionText='重试'
          onAction={() => void load()}
        />
      </View>
    )
  }

  const statusOk = !latestAlert && trend?.latest != null
  const statusWarn = Boolean(latestAlert)
  const statusLabel = statusWarn ? '需关注' : statusOk ? '正常' : '暂无'

  return (
    <View className='health-detail'>
      <View className='health-detail__hero'>
        <View className='health-detail__hero-main'>
          <View className='health-detail__value-row'>
            <Text className='health-detail__value'>
              {formatStatValue(metricCode, trend?.latest)}
            </Text>
            {trend?.latest != null && unit ? (
              <Text className='health-detail__unit'>{unit}</Text>
            ) : null}
          </View>
          <Text className='health-detail__time'>
            {trend?.latestTime
              ? `测量于 ${formatMeasuredTime(trend.latestTime)}`
              : '暂无同步数据'}
          </Text>
        </View>
        <Text
          className={`health-detail__status-pill${
            statusWarn
              ? ' health-detail__status-pill--warn'
              : statusOk
                ? ' health-detail__status-pill--ok'
                : ' health-detail__status-pill--none'
          }`}
        >
          ● {statusLabel}
        </Text>
      </View>

      <View className='health-detail__ranges'>
        {RANGE_OPTIONS.map((opt) => (
          <Text
            key={opt.days}
            className={`health-detail__range${
              rangeDays === opt.days ? ' health-detail__range--on' : ''
            }`}
            onClick={() => onRangeChange(opt.days)}
          >
            {opt.label}
          </Text>
        ))}
      </View>

      {alerts.length > 0 && (
        <View className='health-detail__notice'>
          <Text className='health-detail__notice-title'>异常预警</Text>
          <Text className='health-detail__notice-desc'>
            {alerts.length} 条超出参考范围或明显偏离近期均值，仅供留意，不能替代诊疗。
          </Text>
        </View>
      )}

      <Text className='health-detail__group-title'>趋势</Text>
      <View className='health-detail__group health-detail__group--pad'>
        <TrendSpark points={displayPoints} unit={unit} />
      </View>

      <View className='health-detail__stats'>
        <View className='health-detail__stat'>
          <Text className='health-detail__stat-v'>
            {formatStatValue(metricCode, trend?.avg)}
          </Text>
          <Text className='health-detail__stat-k'>平均</Text>
        </View>
        <View className='health-detail__stat'>
          <Text className='health-detail__stat-v'>
            {formatStatValue(metricCode, trend?.min)}
          </Text>
          <Text className='health-detail__stat-k'>最低</Text>
        </View>
        <View className='health-detail__stat'>
          <Text className='health-detail__stat-v'>
            {formatStatValue(metricCode, trend?.max)}
          </Text>
          <Text className='health-detail__stat-k'>最高</Text>
        </View>
        <View className='health-detail__stat'>
          <Text className='health-detail__stat-v'>{trend?.pointCount || 0}</Text>
          <Text className='health-detail__stat-k'>记录数</Text>
        </View>
      </View>

      <Text className='health-detail__group-title'>测量记录</Text>
      <View className='health-detail__group'>
        {!rows.length ? (
          <View className='health-detail__empty'>
            <Text>暂无记录，同步数据源后可在这里查看</Text>
          </View>
        ) : (
          rows.map(({ sample, alert }, i) => (
            <View
              key={sample.id}
              className={`health-detail__row ${alert ? 'is-alert' : ''} ${
                i === rows.length - 1 ? 'is-last' : ''
              }`}
            >
              <View className='health-detail__row-main'>
                <Text className={`health-detail__row-value ${alert ? 'is-alert' : ''}`}>
                  {formatStatValue(metricCode, sample.valueNum)}
                  {unit ? ` ${unit}` : ''}
                </Text>
                <Text className='health-detail__row-meta'>
                  {formatMeasuredTime(sample.measuredTime)}
                  {sample.providerCode ? ` · ${providerLabel(sample.providerCode)}` : ''}
                </Text>
              </View>
              <View className='health-detail__row-side'>
                {alert ? (
                  <Text className='health-detail__row-badge'>
                    {alert.level === 'high' ? '偏高' : '偏低'}
                  </Text>
                ) : (
                  <Text className='health-detail__row-ok'>正常</Text>
                )}
                {alert ? (
                  <Text className='health-detail__row-warn'>{alert.message}</Text>
                ) : null}
              </View>
            </View>
          ))
        )}
      </View>
    </View>
  )
}
