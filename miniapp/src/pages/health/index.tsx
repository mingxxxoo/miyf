import { View, Text, Input, Button, ScrollView, Picker } from '@tarojs/components'
import Taro, { useDidShow, usePullDownRefresh } from '@tarojs/taro'
import { useEffect, useMemo, useRef, useState } from 'react'
import EmptyState from '@/components/EmptyState'
import Loading from '@/components/Loading'
import ServiceSwitcher from '@/components/ServiceSwitcher'
import TrendSpark from '@/components/TrendSpark'
import {
  createMySample,
  fetchHuaweiAuthorizeUrl,
  fetchHuaweiOAuthStatus,
  fetchMyBindings,
  fetchMyHealth,
  fetchMySamples,
  fetchMyTrend,
  fetchProviders,
  METRIC_OPTIONS,
  METRIC_VALUE_RANGE,
  displayMetricValue,
  providerLabel,
  removeMySample,
  revokeHuaweiOAuth,
  storedMetricUnit,
  syncMyProvider,
  toStoredMetricValue,
  updateMyHealth,
  type HealthProvider,
  type HealthProviderBinding,
  type HealthSample,
  type HealthSubject,
  type HealthTrend
} from '@/api/health'
import { useAuthGuard } from '@/hooks/useAuthGuard'
import { PRODUCT_META, useProductStore } from '@/stores/productStore'
import './index.scss'

const HUAWEI_POLL_MS = 3000
const HUAWEI_POLL_MAX_MS = 3 * 60 * 1000
const GENDER_OPTIONS = ['男', '女', '保密'] as const

function pad2(n: number): string {
  return String(n).padStart(2, '0')
}

function nowDateStr(): string {
  const d = new Date()
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

function nowTimeStr(): string {
  const d = new Date()
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}`
}

function toMeasuredTime(date: string, time: string): string {
  const t = time && time.length >= 4 ? time : '12:00'
  return `${date}T${t}:00`
}

function greetingByHour(): string {
  const h = new Date().getHours()
  if (h < 11) return '早上好，看看今天的身体状态'
  if (h < 14) return '中午好，记一笔刚测的数据'
  if (h < 18) return '下午好，趋势慢慢会清晰'
  return '晚上好，收尾今天的健康记录'
}

function formatMeasuredTime(raw?: string): string {
  if (!raw) return ''
  const d = new Date(raw)
  if (Number.isNaN(d.getTime())) return raw.replace('T', ' ').slice(0, 16)

  const now = new Date()
  const hm = `${pad2(d.getHours())}:${pad2(d.getMinutes())}`

  const startOfDay = (x: Date) => new Date(x.getFullYear(), x.getMonth(), x.getDate()).getTime()
  const diffDays = Math.round((startOfDay(now) - startOfDay(d)) / 86400000)

  if (diffDays === 0) return `今天 ${hm}`
  if (diffDays === 1) return `昨天 ${hm}`
  if (diffDays < 7 && diffDays > 1) return `${diffDays} 天前 ${hm}`
  return `${d.getMonth() + 1}/${d.getDate()} ${hm}`
}

function formatDelta(latest?: number, prev?: number, unit?: string): string | null {
  if (latest == null || prev == null) return null
  const delta = latest - prev
  if (delta === 0) return '较上次持平'
  const sign = delta > 0 ? '+' : ''
  const u = unit ? ` ${unit}` : ''
  return `较上次 ${sign}${Number(delta.toFixed(2))}${u}`
}

function genderIndex(gender?: string): number {
  const i = GENDER_OPTIONS.indexOf((gender || '保密') as (typeof GENDER_OPTIONS)[number])
  return i >= 0 ? i : 2
}

export default function HealthPage() {
  const { isLoggedIn, bootstrapping } = useAuthGuard()
  const setProduct = useProductStore((s) => s.setProduct)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const [trendLoading, setTrendLoading] = useState(false)
  const [metricError, setMetricError] = useState(false)
  const [subject, setSubject] = useState<HealthSubject | null>(null)
  const [samples, setSamples] = useState<HealthSample[]>([])
  const [trend, setTrend] = useState<HealthTrend | null>(null)
  const [metricIndex, setMetricIndex] = useState(0)
  const [value, setValue] = useState('')
  const [measuredDate, setMeasuredDate] = useState(nowDateStr)
  const [measuredTime, setMeasuredTime] = useState(nowTimeStr)
  const [recordOpen, setRecordOpen] = useState(false)
  const [saving, setSaving] = useState(false)
  const [huaweiAuthorized, setHuaweiAuthorized] = useState(false)
  const [huaweiPolling, setHuaweiPolling] = useState(false)
  const [huaweiBusy, setHuaweiBusy] = useState(false)
  const [authorizeUrl, setAuthorizeUrl] = useState('')
  const [trendOpen, setTrendOpen] = useState(true)
  const [profileOpen, setProfileOpen] = useState(false)
  const [profileSaving, setProfileSaving] = useState(false)
  const [profileName, setProfileName] = useState('')
  const [profileGender, setProfileGender] = useState<string>('保密')
  const [profileBirth, setProfileBirth] = useState('')
  const [profileHeight, setProfileHeight] = useState('')
  const [providers, setProviders] = useState<HealthProvider[]>([])
  const [bindings, setBindings] = useState<HealthProviderBinding[]>([])

  const pollTimerRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const pollDeadlineRef = useRef(0)
  const huaweiPollingRef = useRef(false)
  const hasLoadedRef = useRef(false)
  const metricReqRef = useRef(0)
  const metricIndexRef = useRef(0)

  const metric = METRIC_OPTIONS[metricIndex]
  const greeting = useMemo(() => greetingByHour(), [])

  const latestText = useMemo(() => {
    if (!trend || trend.latest == null) return '—'
    const v = displayMetricValue(metric.code, Number(trend.latest))
    const unit = metric.unit ? ` ${metric.unit}` : ''
    return `${v}${unit}`
  }, [trend, metric.code, metric.unit])

  const deltaText = useMemo(() => {
    const points = trend?.points || []
    const nums = points
      .map((p) => (typeof p.value === 'number' ? displayMetricValue(metric.code, p.value) : null))
      .filter((v): v is number => v != null)
    if (nums.length < 2) return null
    return formatDelta(nums[nums.length - 1], nums[nums.length - 2], metric.unit)
  }, [trend, metric.code, metric.unit])

  const displayTrend = useMemo(() => {
    if (!trend) return null
    if (metric.code !== 'SLEEP_MINUTES') return trend
    return {
      ...trend,
      unit: metric.unit,
      latest: trend.latest != null ? displayMetricValue(metric.code, Number(trend.latest)) : trend.latest,
      points: (trend.points || []).map((p) => ({
        ...p,
        value: typeof p.value === 'number' ? displayMetricValue(metric.code, p.value) : p.value
      }))
    }
  }, [trend, metric.code, metric.unit])

  const fillProfileForm = (me: HealthSubject) => {
    setProfileName(me.displayName || '')
    setProfileGender(
      me.gender && GENDER_OPTIONS.includes(me.gender as (typeof GENDER_OPTIONS)[number])
        ? me.gender
        : '保密'
    )
    setProfileBirth(me.birthDate ? String(me.birthDate).slice(0, 10) : '')
    setProfileHeight(me.heightCm != null ? String(me.heightCm) : '')
  }

  const stopHuaweiPoll = () => {
    if (pollTimerRef.current != null) {
      clearInterval(pollTimerRef.current)
      pollTimerRef.current = null
    }
    huaweiPollingRef.current = false
    setHuaweiPolling(false)
  }

  const loadMetricData = async (index: number, opts?: { clearOnStart?: boolean }): Promise<boolean> => {
    const reqId = ++metricReqRef.current
    const code = METRIC_OPTIONS[index].code
    setTrendLoading(true)
    setMetricError(false)
    if (opts?.clearOnStart) {
      setSamples([])
      setTrend(null)
    }
    try {
      const [sampleList, trendData] = await Promise.all([
        fetchMySamples({ metricCode: code, limit: 20 }),
        fetchMyTrend(code, 30)
      ])
      if (reqId !== metricReqRef.current) return false
      setSamples(sampleList)
      setTrend(trendData)
      return true
    } catch {
      if (reqId !== metricReqRef.current) return false
      setMetricError(true)
      Taro.showToast({ title: '指标数据加载失败', icon: 'none' })
      return false
    } finally {
      if (reqId === metricReqRef.current) setTrendLoading(false)
    }
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
      await loadMetricData(metricIndexRef.current)
      return run
    } catch {
      return null
    } finally {
      setHuaweiBusy(false)
    }
  }

  const refreshHuaweiStatus = async (opts?: {
    quiet?: boolean
  }) => {
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
      fillProfileForm(me)
      await Promise.all([
        loadMetricData(metricIndexRef.current),
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

  const onMetricSelect = async (index: number) => {
    if (index === metricIndex) return
    const prevIndex = metricIndex
    metricIndexRef.current = index
    setMetricIndex(index)
    setValue('')
    setRecordOpen(false)
    try {
      Taro.vibrateShort({ type: 'light' })
    } catch {
      // ignore
    }
    const ok = await loadMetricData(index, { clearOnStart: true })
    if (!ok) {
      metricIndexRef.current = prevIndex
      setMetricIndex(prevIndex)
      await loadMetricData(prevIndex)
    }
  }

  const handleCreate = async () => {
    const num = Number(value)
    if (!Number.isFinite(num)) {
      Taro.showToast({ title: '请输入有效数值', icon: 'none' })
      return
    }
    const range = METRIC_VALUE_RANGE[metric.code]
    if (range && (num < range.min || num > range.max)) {
      Taro.showToast({
        title: `请输入 ${range.min}~${range.max} 之间的值`,
        icon: 'none'
      })
      return
    }
    if (!measuredDate) {
      Taro.showToast({ title: '请选择测量日期', icon: 'none' })
      return
    }
    setSaving(true)
    try {
      await createMySample({
        metricCode: metric.code,
        valueNum: toStoredMetricValue(metric.code, num),
        unit: storedMetricUnit(metric.code, metric.unit),
        measuredTime: toMeasuredTime(measuredDate, measuredTime)
      })
      setValue('')
      setMeasuredDate(nowDateStr())
      setMeasuredTime(nowTimeStr())
      setRecordOpen(false)
      Taro.showToast({ title: '已记录', icon: 'success' })
      await loadMetricData(metricIndexRef.current)
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
        const prev = samples
        setSamples((list) => list.filter((s) => s.id !== id))
        try {
          await removeMySample(id)
          await loadMetricData(metricIndexRef.current)
        } catch {
          setSamples(prev)
        }
      }
    })
  }

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
      content: '确认撤销华为健康授权？本地已同步的数据不会删除。',
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

  const saveProfile = async () => {
    const name = profileName.trim()
    if (!name) {
      Taro.showToast({ title: '请填写称呼', icon: 'none' })
      return
    }
    let height: number | undefined
    if (profileHeight.trim()) {
      height = Number(profileHeight)
      if (!Number.isFinite(height) || height <= 0) {
        Taro.showToast({ title: '身高请输入有效数字', icon: 'none' })
        return
      }
    }
    setProfileSaving(true)
    try {
      const me = await updateMyHealth({
        displayName: name,
        gender: profileGender,
        birthDate: profileBirth || undefined,
        heightCm: height
      })
      setSubject(me)
      fillProfileForm(me)
      Taro.showToast({ title: '资料已保存', icon: 'success' })
    } catch {
      // toast from request
    } finally {
      setProfileSaving(false)
    }
  }

  if (bootstrapping || !isLoggedIn) {
    return <Loading fullscreen text='正在登录…' />
  }

  if (loading && !subject) {
    return <Loading fullscreen text='加载健康数据…' />
  }

  if (loadError && !subject) {
    return (
      <View className='health-page'>
        <EmptyState
          title='加载失败'
          description='无法获取健康资料，请检查网络后重试'
          actionText='重试'
          onAction={() => void load()}
        />
      </View>
    )
  }

  return (
    <View className='health-page'>
      <View className='health-page__hero'>
        <ServiceSwitcher compact className='health-page__switch' />
        <Text className='health-page__brand'>{PRODUCT_META.health.brand}</Text>
        <Text className='health-page__greeting'>{greeting}</Text>
        <Text className='health-page__sub'>
          {subject?.displayName ? `${subject.displayName} · 记录与趋势` : '记录体征，看看变化趋势'}
        </Text>
      </View>

      <View className='health-page__card ck-card health-page__profile'>
        <View
          className='health-page__card-head ck-pressable'
          onClick={() => {
            if (!profileOpen && subject) fillProfileForm(subject)
            setProfileOpen((o) => !o)
          }}
        >
          <Text className='health-page__section'>健康资料</Text>
          <Text className='health-page__link'>{profileOpen ? '收起' : '编辑 →'}</Text>
        </View>
        {!profileOpen && subject && (
          <Text className='health-page__profile-summary'>
            {[
              subject.displayName,
              subject.gender,
              subject.birthDate ? String(subject.birthDate).slice(0, 10) : '',
              subject.heightCm != null ? `${subject.heightCm} cm` : ''
            ]
              .filter(Boolean)
              .join(' · ') || '完善称呼、性别、生日与身高'}
          </Text>
        )}
        {profileOpen && (
          <View className='health-page__profile-form'>
            <View className='health-page__field'>
              <Text className='health-page__field-label'>称呼</Text>
              <Input
                className='health-page__input'
                value={profileName}
                placeholder='怎么称呼你'
                onInput={(e) => setProfileName(e.detail.value)}
              />
            </View>
            <View className='health-page__field'>
              <Text className='health-page__field-label'>性别</Text>
              <Picker
                mode='selector'
                range={[...GENDER_OPTIONS]}
                value={genderIndex(profileGender)}
                onChange={(e) => {
                  setProfileGender(GENDER_OPTIONS[Number(e.detail.value)] || '保密')
                }}
              >
                <View className='health-page__picker'>{profileGender || '保密'}</View>
              </Picker>
            </View>
            <View className='health-page__field'>
              <Text className='health-page__field-label'>生日</Text>
              <Picker
                mode='date'
                value={profileBirth || '1990-01-01'}
                onChange={(e) => setProfileBirth(String(e.detail.value))}
              >
                <View className='health-page__picker'>{profileBirth || '选择日期'}</View>
              </Picker>
            </View>
            <View className='health-page__field'>
              <Text className='health-page__field-label'>身高 (cm)</Text>
              <Input
                className='health-page__input'
                type='digit'
                value={profileHeight}
                placeholder='如 170'
                onInput={(e) => setProfileHeight(e.detail.value)}
              />
            </View>
            <Button
              className='ck-btn-health health-page__profile-save'
              size='mini'
              loading={profileSaving}
              disabled={profileSaving}
              onClick={() => void saveProfile()}
            >
              保存资料
            </Button>
          </View>
        )}
      </View>

      <View className='health-page__providers'>
        <Text className='health-page__providers-title'>连接数据源</Text>
        {(providers.length
          ? providers
          : [{ code: 'huawei', displayName: '华为健康', enabled: true, supportsRemoteFetch: true }]
        ).map((p) => {
          const bind = bindings.find((b) => b.providerCode === p.code)
          const connected =
            p.code === 'huawei'
              ? huaweiAuthorized
              : Boolean(bind?.hasCredential || (bind?.status || '').toUpperCase() === 'ACTIVE')
          const syncHint = bind?.lastSyncTime
            ? `上次同步 ${formatMeasuredTime(bind.lastSyncTime)}`
            : ''
          const statusText = !p.enabled
            ? '未启用'
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
            <View key={p.code} className='health-page__provider-row'>
              <View className='health-page__provider-copy'>
                <Text className='health-page__provider-name'>{p.displayName || providerLabel(p.code)}</Text>
                <Text className='health-page__provider-status'>{statusText}</Text>
              </View>
              {p.code === 'manual' && (
                <Text className='health-page__provider-tag'>本机录入</Text>
              )}
            </View>
          )
        })}
      </View>

      <View
        className={`health-page__huawei ${
          huaweiAuthorized ? 'health-page__huawei--on' : ''
        } ${huaweiPolling ? 'health-page__huawei--wait' : ''}`}
      >
        <View className='health-page__huawei-main'>
          <View className='health-page__huawei-copy'>
            <Text className='health-page__huawei-title'>华为健康</Text>
            <Text className='health-page__huawei-desc'>
              {huaweiAuthorized
                ? '已连接，可同步步数、心率等'
                : huaweiPolling
                  ? '请在系统浏览器完成授权后返回本页'
                  : '需复制链接到浏览器授权（小程序内无法直接完成）'}
            </Text>
          </View>
          {!huaweiAuthorized ? (
            <Button
              className='ck-btn-health health-page__huawei-primary'
              size='mini'
              loading={huaweiBusy && !huaweiPolling}
              disabled={huaweiBusy && !huaweiPolling}
              onClick={() => void handleHuaweiConnect()}
            >
              {huaweiPolling ? '重新复制' : '去浏览器授权'}
            </Button>
          ) : (
            <View className='health-page__huawei-ops'>
              <Button
                className='ck-btn-health health-page__huawei-primary'
                size='mini'
                loading={huaweiBusy}
                disabled={huaweiBusy}
                onClick={() => void handleHuaweiSync()}
              >
                同步
              </Button>
              <Text
                className={`health-page__huawei-unlink ${huaweiBusy ? 'is-disabled' : ''}`}
                onClick={() => {
                  if (!huaweiBusy) handleHuaweiRevoke()
                }}
              >
                断开
              </Text>
            </View>
          )}
        </View>

        {!huaweiAuthorized && (
          <Text
            className='health-page__huawei-manual'
            onClick={() => {
              setRecordOpen(true)
              Taro.showToast({ title: '也可先手动录入', icon: 'none' })
            }}
          >
            授权不便？先手动录入 →
          </Text>
        )}

        {huaweiPolling && !huaweiAuthorized && (
          <View className='health-page__oauth-steps'>
            <Text className='health-page__oauth-step'>1 已复制</Text>
            <Text className='health-page__oauth-step'>2 请在浏览器授权</Text>
            <Text className='health-page__oauth-step'>3 返回本页</Text>
            <View className='health-page__oauth-actions'>
              <Button
                className='ck-btn-secondary health-page__oauth-btn'
                size='mini'
                loading={huaweiBusy}
                disabled={huaweiBusy}
                onClick={() => void handleHuaweiRecopy()}
              >
                重新复制链接
              </Button>
              <Button
                className='ck-btn-health health-page__oauth-btn'
                size='mini'
                loading={huaweiBusy}
                disabled={huaweiBusy}
                onClick={() => void handleHuaweiRefresh()}
              >
                我已授权刷新
              </Button>
            </View>
          </View>
        )}
      </View>

      <Text className='health-page__section health-page__section--pad'>记录指标</Text>
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

      <View className={`health-page__focus ${trendLoading ? 'health-page__focus--loading' : ''}`}>
        <View className='health-page__focus-main'>
          <Text className='health-page__focus-label'>{metric.label}</Text>
          <Text className='health-page__focus-value'>{latestText}</Text>
          {deltaText ? (
            <Text className='health-page__focus-delta'>{deltaText}</Text>
          ) : (
            <Text className='health-page__focus-delta health-page__focus-delta--muted'>
              {trend?.latestTime
                ? `更新于 ${formatMeasuredTime(trend.latestTime)}`
                : '暂无近期数据'}
            </Text>
          )}
        </View>
        <View className='health-page__focus-side'>
          <Text className='health-page__focus-side-label'>均值</Text>
          <Text className='health-page__focus-side-value'>
            {trend?.avg != null
              ? `${displayMetricValue(metric.code, Number(trend.avg))}`
              : '—'}
          </Text>
          <Text className='health-page__focus-side-unit'>
            {trend?.pointCount ? `${trend.pointCount} 次` : metric.unit}
          </Text>
        </View>
      </View>

      <View className='health-page__card ck-card'>
        <View className='health-page__card-head'>
          <Text
            className='health-page__section ck-pressable'
            onClick={() => setTrendOpen((o) => !o)}
          >
            {metric.label}趋势 {trendOpen ? '▾' : '▸'}
          </Text>
          <Text
            className='health-page__link'
            onClick={() => {
              setMeasuredDate(nowDateStr())
              setMeasuredTime(nowTimeStr())
              setRecordOpen((open) => !open)
            }}
          >
            {recordOpen ? '收起录入' : '手动录入 →'}
          </Text>
        </View>

        {trendOpen && (
          <>
            {(displayTrend?.min != null || displayTrend?.max != null) && (
              <View className='health-page__range'>
                <Text>
                  最低{' '}
                  {displayTrend?.min != null
                    ? displayMetricValue(metric.code, Number(displayTrend.min))
                    : '—'}
                </Text>
                <Text>
                  最高{' '}
                  {displayTrend?.max != null
                    ? displayMetricValue(metric.code, Number(displayTrend.max))
                    : '—'}
                </Text>
              </View>
            )}
            <TrendSpark
              points={displayTrend?.points || []}
              unit={displayTrend?.unit || metric.unit}
            />
          </>
        )}

        {recordOpen && (
          <View className='health-page__record'>
            <Text className='health-page__hint'>单位 {metric.unit || '—'}</Text>
            <View className='health-page__record-row'>
              <Input
                className='health-page__input health-page__input--inline'
                type='digit'
                placeholder={`输入${metric.label}`}
                value={value}
                onInput={(e) => setValue(e.detail.value)}
                onConfirm={() => void handleCreate()}
              />
              <Button
                className='ck-btn-health health-page__record-btn'
                size='mini'
                loading={saving}
                disabled={saving}
                onClick={() => void handleCreate()}
              >
                保存
              </Button>
            </View>
            <View className='health-page__record-time'>
              <Picker
                mode='date'
                value={measuredDate}
                onChange={(e) => setMeasuredDate(String(e.detail.value))}
              >
                <View className='health-page__picker health-page__picker--half'>
                  {measuredDate || '测量日期'}
                </View>
              </Picker>
              <Picker
                mode='time'
                value={measuredTime || '12:00'}
                onChange={(e) => setMeasuredTime(String(e.detail.value))}
              >
                <View className='health-page__picker health-page__picker--half'>
                  {measuredTime || '12:00'}
                </View>
              </Picker>
            </View>
          </View>
        )}
      </View>

      <View className='health-page__list'>
        <View className='health-page__list-head'>
          <Text className='health-page__section'>最近记录</Text>
          {samples.length > 0 && (
            <Text className='health-page__list-count'>{samples.length} 条</Text>
          )}
        </View>
        {metricError && (
          <View className='health-page__metric-error' onClick={() => void loadMetricData(metricIndex)}>
            <Text>指标加载失败，点此重试</Text>
          </View>
        )}
        {!samples.length ? (
          <EmptyState
            title='还没有这类记录'
            description={
              huaweiAuthorized
                ? `可同步华为，或手动录入${metric.label}`
                : `先手动录入一条${metric.label}，也可连接华为`
            }
            actionText='手动录入'
            onAction={() => {
              setMeasuredDate(nowDateStr())
              setMeasuredTime(nowTimeStr())
              setRecordOpen(true)
            }}
          />
        ) : (
          samples.map((s) => (
            <View key={s.id} className='health-page__item ck-card'>
              <View className='health-page__item-body'>
                <Text className='health-page__item-title'>
                  {displayMetricValue(metric.code, Number(s.valueNum))}
                  {` ${metric.unit || s.unit || ''}`}
                </Text>
                <Text className='health-page__item-meta'>
                  {formatMeasuredTime(s.measuredTime)} · {providerLabel(s.providerCode)}
                </Text>
              </View>
              <Text
                className='health-page__item-del ck-pressable'
                onClick={() => handleDelete(s.id)}
              >
                删除
              </Text>
            </View>
          ))
        )}
      </View>
    </View>
  )
}
