import { View, Text, Button } from '@tarojs/components'
import Taro, { useDidShow, usePullDownRefresh } from '@tarojs/taro'
import { useEffect, useMemo, useRef, useState } from 'react'
import EmptyState from '@/components/EmptyState'
import Loading from '@/components/Loading'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import {
  assessMetricValue,
  fetchHuaweiAuthorizeUrl,
  fetchHuaweiOAuthStatus,
  fetchMyBindings,
  fetchMyHealth,
  fetchMyTrend,
  fetchProviders,
  formatMeasuredTime,
  formatStatValue,
  METRIC_OPTIONS,
  providerLabel,
  revokeHuaweiOAuth,
  syncMyProvider,
  type HealthProvider,
  type HealthProviderBinding,
  type HealthSubject,
  type HealthTrend
} from '@/api/health'
import { useAuthGuard } from '@/hooks/useAuthGuard'
import { PRODUCT_META, useProductStore } from '@/stores/productStore'
import './index.scss'

const HUAWEI_POLL_MS = 3000
const HUAWEI_POLL_MAX_MS = 3 * 60 * 1000

const METRIC_ICONS: Record<string, { icon: string; bg: string }> = {
  WEIGHT: { icon: '⚖️', bg: '#E7F7F0' },
  HEART_RATE: { icon: '💗', bg: '#FCEEEA' },
  STEPS: { icon: '👟', bg: '#FBF3E0' },
  BLOOD_PRESSURE_SYS: { icon: '🩺', bg: '#EBF3FB' },
  BLOOD_PRESSURE_DIA: { icon: '💉', bg: '#EBF3FB' },
  BLOOD_GLUCOSE: { icon: '🍬', bg: '#FFF1E2' },
  BODY_FAT: { icon: '🫧', bg: '#F0EBFB' },
  BMI: { icon: '📏', bg: '#E7F7F0' },
  HEIGHT: { icon: '📐', bg: '#F4F0E9' },
  SPO2: { icon: '🫁', bg: '#EBF3FB' },
  SLEEP_MINUTES: { icon: '😴', bg: '#F0EBFB' },
  STRESS: { icon: '😮‍💨', bg: '#FBF3E0' },
  CALORIES: { icon: '🔥', bg: '#FFF1E2' }
}

function MiniSpark({
  points,
  warn
}: {
  points?: { value?: number }[]
  warn?: boolean
}) {
  const bars = (points || [])
    .map((p) => (typeof p.value === 'number' ? p.value : null))
    .filter((v): v is number => v != null)
    .slice(-8)
  if (bars.length < 2) {
    return <View className='health-page__spark health-page__spark--empty' />
  }
  const min = Math.min(...bars)
  const max = Math.max(...bars)
  const span = max - min || 1
  return (
    <View className={`health-page__spark ${warn ? 'health-page__spark--warn' : ''}`}>
      {bars.map((v, i) => {
        const h = Math.round(8 + ((v - min) / span) * 24)
        return <View key={i} className='health-page__spark-bar' style={{ height: `${h}px` }} />
      })}
    </View>
  )
}

export default function HealthPage() {
  const { isLoggedIn, bootstrapping } = useAuthGuard()
  const setProduct = useProductStore((s) => s.setProduct)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const [subject, setSubject] = useState<HealthSubject | null>(null)
  const [trends, setTrends] = useState<Record<string, HealthTrend | null>>({})
  const [huaweiAuthorized, setHuaweiAuthorized] = useState(false)
  const [huaweiPolling, setHuaweiPolling] = useState(false)
  const [huaweiBusy, setHuaweiBusy] = useState(false)
  const [authorizeUrl, setAuthorizeUrl] = useState('')
  const [providers, setProviders] = useState<HealthProvider[]>([])
  const [bindings, setBindings] = useState<HealthProviderBinding[]>([])

  const pollTimerRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const pollDeadlineRef = useRef(0)
  const huaweiPollingRef = useRef(false)
  const hasLoadedRef = useRef(false)

  const sourceList = useMemo(() => {
    const fallback: HealthProvider[] = [
      { code: 'huawei', displayName: '华为健康', enabled: true, supportsRemoteFetch: true }
    ]
    const list = (providers.length ? providers : fallback).filter((p) => p.code && p.code !== 'manual')
    if (!list.some((p) => p.code === 'huawei')) {
      return [...list, fallback[0]]
    }
    return list
  }, [providers])

  const cards = useMemo(() => {
    return METRIC_OPTIONS.map((m) => {
      const trend = trends[m.code]
      const latestAlert =
        trend?.latest != null ? assessMetricValue(m.code, trend.latest) : null
      return { metric: m, trend: trend || null, latestAlert }
    })
  }, [trends])

  const summary = useMemo(() => {
    const withData = cards.filter((c) => (c.trend?.pointCount || 0) > 0).length
    const alerts = cards.filter((c) => c.latestAlert).length
    return { withData, alerts }
  }, [cards])

  const healthScore = useMemo(() => {
    if (!summary.withData) return 0
    const base = 72
    const bonus = Math.min(28, summary.withData * 4)
    const penalty = summary.alerts * 10
    return Math.max(35, Math.min(98, base + bonus - penalty))
  }, [summary])

  const scoreTitle = useMemo(() => {
    if (!summary.withData) return '暂无数据'
    if (summary.alerts === 0) return '状态不错，继续保持 🌿'
    if (summary.alerts <= 2) return '需要留意一下'
    return '建议多关注身体'
  }, [summary])

  const firstAlert = useMemo(() => {
    return cards.find((c) => c.latestAlert) || null
  }, [cards])

  const latestSyncHint = useMemo(() => {
    const times = cards
      .map((c) => c.trend?.latestTime)
      .filter(Boolean)
      .map((t) => new Date(String(t).replace(' ', 'T')).getTime())
      .filter((n) => Number.isFinite(n))
    if (!times.length) return ''
    const latest = Math.max(...times)
    return `数据更新至 ${formatMeasuredTime(new Date(latest).toISOString())}`
  }, [cards])

  const stopHuaweiPoll = () => {
    if (pollTimerRef.current != null) {
      clearInterval(pollTimerRef.current)
      pollTimerRef.current = null
    }
    huaweiPollingRef.current = false
    setHuaweiPolling(false)
  }

  const loadTrends = async () => {
    const results = await Promise.all(
      METRIC_OPTIONS.map((m) =>
        fetchMyTrend(m.code, 30, { showError: false })
          .then((trend) => ({ code: m.code, trend }))
          .catch(() => ({ code: m.code, trend: null as HealthTrend | null }))
      )
    )
    const next: Record<string, HealthTrend | null> = {}
    results.forEach((row) => {
      next[row.code] = row.trend
    })
    setTrends(next)
  }

  const handleHuaweiSync = async (opts?: { quiet?: boolean }) => {
    setHuaweiBusy(true)
    try {
      const run = await syncMyProvider('huawei')
      if (!opts?.quiet) {
        if (run.status === 'FAILED') {
          Taro.showToast({
            title: (run.errorMessage || '同步失败').slice(0, 40),
            icon: 'none'
          })
        } else {
          const ingested = run.ingestedCount != null ? `，入库 ${run.ingestedCount} 条` : ''
          Taro.showToast({
            title: `同步成功${ingested}`.slice(0, 40),
            icon: 'success'
          })
        }
      }
      await loadTrends()
      return run
    } catch {
      return null
    } finally {
      setHuaweiBusy(false)
    }
  }

  const refreshHuaweiStatus = async (opts?: { quiet?: boolean }) => {
    try {
      const st = await fetchHuaweiOAuthStatus({ showError: !opts?.quiet })
      setHuaweiAuthorized(st.authorized)
      if (st.authorized && huaweiPollingRef.current) {
        stopHuaweiPoll()
        setAuthorizeUrl('')
        Taro.showToast({ title: '华为已连接，正在同步', icon: 'none' })
        void handleHuaweiSync({ quiet: true })
      }
      return st.authorized
    } catch {
      if (!opts?.quiet) {
        setHuaweiAuthorized(false)
      }
      return false
    }
  }

  const startHuaweiPoll = () => {
    stopHuaweiPoll()
    pollDeadlineRef.current = Date.now() + HUAWEI_POLL_MAX_MS
    huaweiPollingRef.current = true
    setHuaweiPolling(true)
    pollTimerRef.current = setInterval(() => {
      if (Date.now() > pollDeadlineRef.current) {
        stopHuaweiPoll()
        Taro.showToast({ title: '仍未检测到授权，可下拉刷新', icon: 'none' })
        return
      }
      void refreshHuaweiStatus({ quiet: true })
    }, HUAWEI_POLL_MS)
  }

  const load = async (opts?: { soft?: boolean }) => {
    const soft = Boolean(opts?.soft && hasLoadedRef.current)
    if (!soft) setLoading(true)
    setLoadError(false)
    try {
      const me = await fetchMyHealth()
      setSubject(me)
      await Promise.all([
        loadTrends(),
        refreshHuaweiStatus({ quiet: true }),
        fetchProviders()
          .then((list) => setProviders(list.filter((p) => p.code)))
          .catch(() => setProviders([])),
        fetchMyBindings()
          .then((list) => setBindings(list))
          .catch(() => setBindings([]))
      ])
      hasLoadedRef.current = true
    } catch {
      if (!soft || !hasLoadedRef.current) {
        setLoadError(true)
        setSubject(null)
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    return () => stopHuaweiPoll()
  }, [])

  useDidShow(() => {
    setProduct('health')
    Taro.setNavigationBarTitle({ title: PRODUCT_META.health.brand })
    if (bootstrapping || !isLoggedIn) return
    if (huaweiPollingRef.current) {
      void refreshHuaweiStatus({ quiet: true })
    }
    void load({ soft: true })
  })

  usePullDownRefresh(async () => {
    try {
      await load({ soft: true })
    } finally {
      Taro.stopPullDownRefresh()
    }
  })

  const copyAuthorizeUrl = async (url: string) => {
    if (!url) return
    await Taro.setClipboardData({ data: url })
    Taro.showToast({ title: '链接已复制', icon: 'none' })
  }

  const handleHuaweiConnect = async () => {
    setHuaweiBusy(true)
    try {
      const { authorizeUrl: url } = await fetchHuaweiAuthorizeUrl()
      if (!url) {
        Taro.showToast({ title: '未返回授权地址', icon: 'none' })
        return
      }
      setAuthorizeUrl(url)
      if (process.env.TARO_ENV === 'h5' && typeof window !== 'undefined') {
        startHuaweiPoll()
        window.location.href = url
        return
      }
      await Taro.setClipboardData({ data: url })
      startHuaweiPoll()
    } catch {
      // ignore
    } finally {
      setHuaweiBusy(false)
    }
  }

  const handleHuaweiRecopy = async () => {
    if (authorizeUrl) {
      await copyAuthorizeUrl(authorizeUrl)
      if (!huaweiPollingRef.current) startHuaweiPoll()
      return
    }
    void handleHuaweiConnect()
  }

  const handleHuaweiRefresh = async () => {
    setHuaweiBusy(true)
    try {
      const ok = await refreshHuaweiStatus({ quiet: false })
      if (!ok) {
        Taro.showToast({ title: '尚未检测到授权', icon: 'none' })
      }
    } finally {
      setHuaweiBusy(false)
    }
  }

  const handleHuaweiRevoke = () => {
    Taro.showModal({
      title: '断开华为',
      content: '确认撤销华为健康授权？已同步的数据不会删除。',
      success: async (res) => {
        if (!res.confirm) return
        setHuaweiBusy(true)
        try {
          stopHuaweiPoll()
          await revokeHuaweiOAuth()
          setHuaweiAuthorized(false)
          setAuthorizeUrl('')
          Taro.showToast({ title: '已断开', icon: 'success' })
        } catch {
          // ignore
        } finally {
          setHuaweiBusy(false)
        }
      }
    })
  }

  const openDetail = (metricCode: string) => {
    Taro.navigateTo({
      url: `/pages/health/detail?metric=${encodeURIComponent(metricCode)}`
    })
  }

  if (bootstrapping || !isLoggedIn) {
    return <Loading fullscreen text='正在登录…' />
  }

  if (loading && !subject) {
    return <Loading fullscreen text='加载健康统计…' />
  }

  if (loadError && !subject) {
    return (
      <View className='health-page'>
        <EmptyState
          title='加载失败'
          description='无法获取健康统计，请检查网络后重试'
          actionText='重试'
          onAction={() => void load()}
        />
      </View>
    )
  }

  return (
    <View className='health-page'>
      <View className='health-page__head'>
        <ServiceSwitcher compact className='health-page__switch' />
        {latestSyncHint ? (
          <Text className='health-page__sync-hint'>{latestSyncHint}</Text>
        ) : null}
      </View>

      <View className='health-page__score'>
        <View className='health-page__ring'>
          <Text className='health-page__ring-val'>{healthScore || '—'}</Text>
          <Text className='health-page__ring-label'>健康评分</Text>
        </View>
        <View className='health-page__score-info'>
          <Text className='health-page__score-title'>{scoreTitle}</Text>
          <Text className='health-page__score-desc'>
            {summary.withData
              ? `${summary.withData} 项指标中 ${Math.max(0, summary.withData - summary.alerts)} 项正常${
                  summary.alerts > 0 ? `，${summary.alerts} 项需关注` : ''
                }`
              : '连接数据源后，这里会汇总你的健康状态'}
          </Text>
          <View className='health-page__score-tags'>
            {summary.withData > 0 ? (
              <Text className='health-page__score-tag'>
                {Math.max(0, summary.withData - summary.alerts)} 正常
              </Text>
            ) : null}
            {summary.alerts > 0 ? (
              <Text className='health-page__score-tag'>{summary.alerts} 需关注</Text>
            ) : null}
          </View>
        </View>
      </View>

      {firstAlert ? (
        <View
          className='health-page__notice ck-pressable'
          onClick={() => openDetail(firstAlert.metric.code)}
        >
          <View className='health-page__notice-ico'>
            <Text>⚠️</Text>
          </View>
          <View className='health-page__notice-body'>
            <Text className='health-page__notice-title'>
              {firstAlert.metric.label}
              {firstAlert.latestAlert?.level === 'high' ? '偏高' : '偏低'}
            </Text>
            <Text className='health-page__notice-text'>
              {firstAlert.latestAlert?.message || '最新值超出参考范围，注意放松与复查'}
            </Text>
          </View>
          <Text className='health-page__notice-go'>查看 ›</Text>
        </View>
      ) : null}

      <View className='health-page__sec-row'>
        <Text className='health-page__group-title'>我的指标</Text>
      </View>
      <View className='health-page__metric-grid'>
        {cards.map(({ metric, trend, latestAlert }) => {
          const meta = METRIC_ICONS[metric.code] || { icon: '📌', bg: '#F4F0E9' }
          const warn = Boolean(latestAlert)
          return (
            <View
              key={metric.code}
              className={`health-page__m-card ck-pressable ${
                warn ? 'health-page__m-card--warn' : ''
              }`}
              onClick={() => openDetail(metric.code)}
            >
              <View className='health-page__m-top'>
                <View className='health-page__m-ico' style={{ background: meta.bg }}>
                  <Text>{meta.icon}</Text>
                </View>
                <Text className='health-page__m-name'>{metric.label}</Text>
                {latestAlert ? (
                  <Text className='ck-chip ck-chip--warn'>需关注</Text>
                ) : trend?.pointCount ? (
                  <Text className='ck-chip ck-chip--ok'>正常</Text>
                ) : (
                  <Text className='ck-chip ck-chip--none'>暂无</Text>
                )}
              </View>
              <View className='health-page__m-val'>
                <Text
                  className={`health-page__m-num ${warn ? 'health-page__m-num--warn' : ''}`}
                >
                  {trend?.latest != null ? formatStatValue(metric.code, trend.latest) : '—'}
                </Text>
                {metric.unit ? <Text className='health-page__m-unit'>{metric.unit}</Text> : null}
              </View>
              <View className='health-page__m-foot'>
                <MiniSpark points={trend?.points} warn={warn} />
                <Text className='health-page__m-time'>
                  {trend?.latestTime ? formatMeasuredTime(trend.latestTime) : '暂无数据'}
                </Text>
              </View>
            </View>
          )
        })}
      </View>

      <Text className='health-page__group-title'>数据源</Text>
      <View className='health-page__group'>
        {sourceList.map((p, i) => {
          const bind = bindings.find((b) => b.providerCode === p.code)
          const connected =
            p.code === 'huawei'
              ? huaweiAuthorized
              : Boolean(bind?.hasCredential || (bind?.status || '').toUpperCase() === 'ACTIVE')
          const syncHint = bind?.lastSyncTime
            ? `上次同步 ${formatMeasuredTime(bind.lastSyncTime)}`
            : ''
          const huaweiDesc = huaweiAuthorized
            ? '已连接，可同步步数、心率、睡眠、压力等'
            : huaweiPolling
              ? '请在系统浏览器完成授权后返回本页'
              : '需复制链接到浏览器授权'
          const statusText = !p.enabled
            ? '未启用'
            : p.code === 'huawei'
              ? [huaweiDesc, connected ? syncHint : ''].filter(Boolean).join(' · ')
              : connected
                ? [
                    bind?.externalAccountMasked
                      ? `已连接 · ${bind.externalAccountMasked}`
                      : '已连接',
                    syncHint
                  ]
                    .filter(Boolean)
                    .join(' · ')
                : '未连接'
          return (
            <View
              key={p.code}
              className={`health-page__cell ${
                i === sourceList.length - 1 && !huaweiPolling ? 'is-last' : ''
              } ${!connected ? 'off' : ''}`}
            >
              <View className='health-page__cell-main'>
                <View className='health-page__cell-copy'>
                  <Text className='health-page__cell-title'>
                    {p.displayName || providerLabel(p.code)}
                  </Text>
                  <Text className='health-page__cell-desc'>{statusText}</Text>
                </View>
                {p.code === 'huawei' && (
                  <>
                    {!huaweiAuthorized ? (
                      <Button
                        className='health-page__mini-btn health-page__mini-btn--primary'
                        size='mini'
                        hoverClass='health-btn-hover'
                        loading={huaweiBusy && !huaweiPolling}
                        disabled={huaweiBusy && !huaweiPolling}
                        onClick={() =>
                          void (huaweiPolling ? handleHuaweiRecopy() : handleHuaweiConnect())
                        }
                      >
                        {huaweiPolling ? '重新复制' : '去授权'}
                      </Button>
                    ) : (
                      <View className='health-page__ops'>
                        <Button
                          className='health-page__mini-btn health-page__mini-btn--primary'
                          size='mini'
                          hoverClass='health-btn-hover'
                          loading={huaweiBusy}
                          disabled={huaweiBusy}
                          onClick={() => void handleHuaweiSync()}
                        >
                          同步
                        </Button>
                        <Text
                          className={`health-page__link ${huaweiBusy ? 'is-disabled' : ''}`}
                          onClick={() => {
                            if (!huaweiBusy) handleHuaweiRevoke()
                          }}
                        >
                          断开
                        </Text>
                      </View>
                    )}
                  </>
                )}
              </View>
            </View>
          )
        })}

        {huaweiPolling && !huaweiAuthorized && (
          <View className='health-page__oauth'>
            <Text className='health-page__oauth-step'>1. 已复制授权链接</Text>
            <Text className='health-page__oauth-step'>2. 在系统浏览器完成授权</Text>
            <Text className='health-page__oauth-step'>3. 返回本页，点「我已授权」</Text>
            <View className='health-page__oauth-actions'>
              <Button
                className='health-page__mini-btn'
                hoverClass='health-btn-hover'
                loading={huaweiBusy}
                disabled={huaweiBusy}
                onClick={() => void handleHuaweiRecopy()}
              >
                重新复制
              </Button>
              <Button
                className='health-page__mini-btn health-page__mini-btn--primary'
                hoverClass='health-btn-hover'
                loading={huaweiBusy}
                disabled={huaweiBusy}
                onClick={() => void handleHuaweiRefresh()}
              >
                我已授权
              </Button>
            </View>
          </View>
        )}
      </View>
    </View>
  )
}
